package vibe.ccc.aichat.data.network

import org.json.JSONArray
import org.json.JSONObject
import vibe.ccc.aichat.data.model.AppUser
import vibe.ccc.aichat.data.model.ChatHistoryResult
import vibe.ccc.aichat.data.model.ChatMessage
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.model.ChatUsage
import vibe.ccc.aichat.data.model.MessageSender

object JsonParsers {
    fun parseAppUser(json: JSONObject): AppUser =
        AppUser(
            id = json.getInt("id"),
            countryCode = json.optString("countryCode"),
            phoneNumber = json.optString("phoneNumber")
        )

    fun parseRoles(array: JSONArray): List<ChatRole> =
        buildList {
            for (index in 0 until array.length()) {
                add(parseChatRole(array.getJSONObject(index)))
            }
        }

    fun parseChatRole(json: JSONObject): ChatRole =
        ChatRole(
            id = json.getInt("id"),
            key = json.optString("key"),
            nickname = json.optString("nickname"),
            description = json.optString("description"),
            avatarUrl = json.optStringOrNull("avatarUrl"),
            backgroundUrl = json.optStringOrNull("backgroundUrl")
        )

    fun parseChatHistoryResult(json: JSONObject): ChatHistoryResult =
        ChatHistoryResult(
            conversationId = json.optIntOrNull("conversationId"),
            roleId = json.getInt("roleId"),
            messages = parseMessages(json.optJSONArray("messages") ?: JSONArray()),
            hasMore = json.optBoolean("hasMore"),
            nextBeforeId = json.optIntOrNull("nextBeforeId")
        )

    fun parseMessages(array: JSONArray): List<ChatMessage> =
        buildList {
            for (index in 0 until array.length()) {
                add(parseChatMessage(array.getJSONObject(index)))
            }
        }

    fun parseChatMessage(json: JSONObject): ChatMessage =
        ChatMessage(
            id = json.getInt("id"),
            sender = MessageSender.fromRawValue(json.optString("sender")),
            content = json.optString("content"),
            createdAt = json.optStringOrNull("createdAt")
        )

    fun parseChatUsage(json: JSONObject): ChatUsage =
        ChatUsage(totalTokens = json.optIntOrNull("total_tokens"))
}

fun JSONObject.optStringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null

fun JSONObject.optIntOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null
