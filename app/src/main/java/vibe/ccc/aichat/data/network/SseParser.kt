package vibe.ccc.aichat.data.network

import org.json.JSONException
import org.json.JSONObject
import vibe.ccc.aichat.data.model.APIError
import vibe.ccc.aichat.data.model.ChatDeltaEvent
import vibe.ccc.aichat.data.model.ChatDoneEvent
import vibe.ccc.aichat.data.model.ChatErrorEvent
import vibe.ccc.aichat.data.model.ChatStartEvent
import vibe.ccc.aichat.data.model.ChatStreamEvent

class SseParser {
    suspend fun appendChunk(
        chunk: String,
        buffer: StringBuilder,
        onEvent: suspend (ChatStreamEvent) -> Unit
    ) {
        buffer.append(chunk.replace("\r\n", "\n"))
        while (true) {
            val index = buffer.indexOf("\n\n")
            if (index < 0) return
            val block = buffer.substring(0, index)
            buffer.delete(0, index + 2)
            parseBlock(block)?.let { onEvent(it) }
        }
    }

    suspend fun flush(buffer: StringBuilder, onEvent: suspend (ChatStreamEvent) -> Unit) {
        val remaining = buffer.toString().trim()
        if (remaining.isNotEmpty()) {
            parseBlock(remaining)?.let { onEvent(it) }
        }
        buffer.clear()
    }

    fun parseBlock(block: String): ChatStreamEvent? {
        var eventName = ""
        val dataLines = mutableListOf<String>()

        block.split("\n").forEach { line ->
            when {
                line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                line.startsWith("data:") -> dataLines += line.removePrefix("data:").trim()
            }
        }

        val data = dataLines.joinToString("\n")
        if (data.isBlank()) return null

        return try {
            val json = JSONObject(data)
            when (eventName) {
                "start" -> ChatStreamEvent.Start(
                    ChatStartEvent(
                        conversationId = json.getInt("conversationId"),
                        roleId = json.getInt("roleId"),
                        userMessage = JsonParsers.parseChatMessage(json.getJSONObject("userMessage"))
                    )
                )
                "delta" -> ChatStreamEvent.Delta(ChatDeltaEvent(json.optString("content")).content)
                "done" -> ChatStreamEvent.Done(
                    ChatDoneEvent(
                        assistantMessage = JsonParsers.parseChatMessage(json.getJSONObject("assistantMessage")),
                        usage = json.optJSONObject("usage")?.let(JsonParsers::parseChatUsage)
                    )
                )
                "error" -> {
                    val error = ChatErrorEvent(
                        code = json.optString("code"),
                        message = json.optString("message", "请求失败，请稍后重试")
                    )
                    ChatStreamEvent.Failure(APIError.Server(error.code, error.message))
                }
                else -> null
            }
        } catch (_: JSONException) {
            ChatStreamEvent.Failure(APIError.Transport("解析流式响应失败"))
        }
    }
}
