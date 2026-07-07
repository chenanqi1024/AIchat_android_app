package vibe.ccc.aichat.data.model

data class APIEnvelope<T>(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null,
    val data: T? = null
)

data class SendCodeResult(
    val bizId: String? = null,
    val expiresIn: Int,
    val retryAfter: Int
)

data class LoginSession(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: AppUser
)

data class AppUser(
    val id: Int,
    val countryCode: String,
    val phoneNumber: String
)

data class RolesResult(
    val roles: List<ChatRole>
)

data class ChatRole(
    val id: Int,
    val key: String,
    val nickname: String,
    val description: String,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null
)

enum class MessageSender(val rawValue: String) {
    User("user"),
    Assistant("assistant");

    companion object {
        fun fromRawValue(value: String): MessageSender =
            entries.firstOrNull { it.rawValue == value } ?: Assistant
    }
}

data class ChatMessage(
    val id: Int,
    val sender: MessageSender,
    val content: String,
    val createdAt: String? = null,
    val localImageData: ByteArray? = null
) {
    fun withContent(newContent: String): ChatMessage = copy(content = newContent)
}

data class ChatHistoryResult(
    val conversationId: Int? = null,
    val roleId: Int,
    val messages: List<ChatMessage>,
    val hasMore: Boolean,
    val nextBeforeId: Int? = null
)

data class ClearHistoryResult(
    val conversationId: Int? = null,
    val roleId: Int,
    val deletedCount: Int
)

data class ChatStartEvent(
    val conversationId: Int,
    val roleId: Int,
    val userMessage: ChatMessage
)

data class ChatDeltaEvent(
    val content: String
)

data class ChatDoneEvent(
    val assistantMessage: ChatMessage,
    val usage: ChatUsage? = null
)

data class ChatUsage(
    val totalTokens: Int? = null
)

data class ChatErrorEvent(
    val code: String,
    val message: String
)

sealed interface ChatStreamEvent {
    data class Start(val event: ChatStartEvent) : ChatStreamEvent
    data class Delta(val content: String) : ChatStreamEvent
    data class Done(val event: ChatDoneEvent) : ChatStreamEvent
    data class Failure(val error: APIError) : ChatStreamEvent
}
