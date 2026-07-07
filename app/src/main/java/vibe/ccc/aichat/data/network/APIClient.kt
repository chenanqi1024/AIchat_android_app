package vibe.ccc.aichat.data.network

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vibe.ccc.aichat.data.model.APIError
import vibe.ccc.aichat.data.model.ChatHistoryResult
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.model.ChatStreamEvent
import vibe.ccc.aichat.data.model.ClearHistoryResult
import vibe.ccc.aichat.data.model.LoginSession
import vibe.ccc.aichat.data.model.RolesResult
import vibe.ccc.aichat.data.model.SendCodeResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class APIClient(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val sseParser = SseParser()

    suspend fun sendCode(
        countryCode: String = "86",
        phoneNumber: String
    ): SendCodeResult {
        val data = requestJsonObject(
            baseUrl = APIConfig.LOGIN_BASE_URL,
            path = "/send-code",
            method = HttpMethod.Post,
            body = JSONObject()
                .put("countryCode", countryCode)
                .put("phoneNumber", phoneNumber)
        )
        return SendCodeResult(
            bizId = data.optStringOrNull("bizId"),
            expiresIn = data.optInt("expiresIn"),
            retryAfter = data.optInt("retryAfter")
        )
    }

    suspend fun login(
        countryCode: String = "86",
        phoneNumber: String,
        verifyCode: String
    ): LoginSession {
        val data = requestJsonObject(
            baseUrl = APIConfig.LOGIN_BASE_URL,
            path = "/login",
            method = HttpMethod.Post,
            body = JSONObject()
                .put("countryCode", countryCode)
                .put("phoneNumber", phoneNumber)
                .put("verifyCode", verifyCode)
        )
        return LoginSession(
            accessToken = data.getString("accessToken"),
            tokenType = data.optString("tokenType"),
            expiresIn = data.optInt("expiresIn"),
            user = JsonParsers.parseAppUser(data.getJSONObject("user"))
        )
    }

    suspend fun fetchRoles(): List<ChatRole> {
        val data = requestJsonObject(
            baseUrl = APIConfig.CHAT_BASE_URL,
            path = "/roles",
            method = HttpMethod.Get
        )
        return RolesResult(JsonParsers.parseRoles(data.optJSONArray("roles") ?: JSONArray())).roles
    }

    suspend fun fetchHistory(
        roleId: Int,
        beforeId: Int? = null,
        limit: Int = 50,
        token: String
    ): ChatHistoryResult {
        val query = buildMap {
            put("roleId", roleId.toString())
            put("limit", limit.coerceIn(1, 100).toString())
            beforeId?.let { put("beforeId", it.toString()) }
        }
        return JsonParsers.parseChatHistoryResult(
            requestJsonObject(
                baseUrl = APIConfig.CHAT_BASE_URL,
                path = "/history",
                method = HttpMethod.Get,
                query = query,
                token = token
            )
        )
    }

    suspend fun clearHistory(roleId: Int, token: String): ClearHistoryResult {
        val data = requestJsonObject(
            baseUrl = APIConfig.CHAT_BASE_URL,
            path = "/history",
            method = HttpMethod.Delete,
            query = mapOf("roleId" to roleId.toString()),
            token = token
        )
        return ClearHistoryResult(
            conversationId = data.optIntOrNull("conversationId"),
            roleId = data.getInt("roleId"),
            deletedCount = data.optInt("deletedCount")
        )
    }

    fun streamChat(
        roleId: Int,
        message: String?,
        imageDataUrl: String? = null,
        token: String
    ): Flow<ChatStreamEvent> = flow {
        val body = JSONObject()
            .put("roleId", roleId)
            .put("message", message ?: JSONObject.NULL)
            .put("image", imageDataUrl ?: JSONObject.NULL)
            .put("stream", true)

        val request = Request.Builder()
            .url("${APIConfig.CHAT_BASE_URL}/chat")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = execute(request)
        response.use {
            if (!it.isSuccessful) throw parseErrorResponse(it)

            val source = it.body?.source() ?: throw APIError.Transport("服务器无响应")
            val buffer = StringBuilder()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                sseParser.appendChunk("$line\n", buffer) { event -> emit(event) }
            }
            sseParser.flush(buffer) { event -> emit(event) }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun requestJsonObject(
        baseUrl: String,
        path: String,
        method: HttpMethod,
        query: Map<String, String> = emptyMap(),
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject {
        val urlBuilder = (baseUrl + path).toHttpUrl().newBuilder()
        query.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")

        token?.let { builder.header("Authorization", "Bearer $it") }

        when (method) {
            HttpMethod.Get -> builder.get()
            HttpMethod.Post -> {
                builder.header("Content-Type", "application/json")
                builder.post((body ?: JSONObject()).toString().toRequestBody(jsonMediaType))
            }
            HttpMethod.Delete -> builder.delete()
        }

        val response = execute(builder.build())
        response.use {
            if (!it.isSuccessful) throw parseErrorResponse(it)

            val responseBody = it.body?.string().orEmpty()
            try {
                val envelope = JSONObject(responseBody)
                if (!envelope.optBoolean("success")) {
                    throw APIError.Server(
                        code = envelope.optString("code", "UNKNOWN_ERROR"),
                        message = envelope.optString("message", "请求失败，请稍后重试")
                    )
                }
                if (!envelope.has("data") || envelope.isNull("data")) {
                    throw APIError.MissingData
                }
                return envelope.getJSONObject("data")
            } catch (error: APIError) {
                throw error
            } catch (error: JSONException) {
                throw APIError.Transport(error.localizedMessage ?: "响应解析失败")
            }
        }
    }

    private suspend fun execute(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(APIError.Transport(e.localizedMessage ?: "网络请求失败"))
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private fun parseErrorResponse(response: Response): APIError {
        val statusCode = response.code
        val responseBody = response.body?.string().orEmpty()
        return try {
            val envelope = JSONObject(responseBody)
            APIError.Server(
                code = envelope.optString("code", "HTTP_$statusCode"),
                message = envelope.optString("message", "请求失败，请稍后重试")
            )
        } catch (_: Exception) {
            APIError.Transport("请求失败（HTTP $statusCode）")
        }
    }

    private enum class HttpMethod {
        Get,
        Post,
        Delete
    }
}
