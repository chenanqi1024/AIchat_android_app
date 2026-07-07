package vibe.ccc.aichat.data.model

sealed class APIError(message: String) : Exception(message) {
    data object MissingData : APIError("服务器返回数据为空")
    data object MissingToken : APIError("请先登录后继续")
    data class Server(val code: String, override val message: String) : APIError(message)
    data class Transport(override val message: String) : APIError(message)

    val requiresLogin: Boolean
        get() = when (this) {
            is Server -> code in setOf("AUTH_REQUIRED", "INVALID_TOKEN", "TOKEN_EXPIRED")
            MissingToken -> true
            MissingData, is Transport -> false
        }
}
