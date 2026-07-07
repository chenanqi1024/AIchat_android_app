package vibe.ccc.aichat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.auth.TestAccount
import vibe.ccc.aichat.data.model.APIError
import vibe.ccc.aichat.data.model.ChatMessage
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.model.ChatStreamEvent
import vibe.ccc.aichat.data.model.MessageSender
import vibe.ccc.aichat.data.network.APIClient

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val errorMessage: String? = null,
    val isLoadingHistory: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    val isSending: Boolean = false,
    val hasMore: Boolean = false
)

sealed interface ChatEffect {
    data object AuthExpired : ChatEffect
}

class ChatViewModel(
    val role: ChatRole,
    private val apiClient: APIClient,
    private val authStore: AuthStore
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private val _effects = MutableSharedFlow<ChatEffect>()
    val effects: SharedFlow<ChatEffect> = _effects

    private var nextBeforeId: Int? = null
    private var streamJob: Job? = null
    private var pendingUserMessageId: Int? = null
    private var pendingAssistantMessageId: Int? = null
    private var nextTemporaryId = -1

    fun updateDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun loadHistory() {
        val token = authStore.state.value.accessToken
        if (token.isNullOrBlank()) {
            requestLogin()
            return
        }
        if (TestAccount.isLegacyToken(token)) {
            requestLogin()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true, errorMessage = null) }
            try {
                val result = apiClient.fetchHistory(roleId = role.id, token = token)
                nextBeforeId = result.nextBeforeId
                _state.update {
                    it.copy(
                        messages = result.messages,
                        hasMore = result.hasMore,
                        isLoadingHistory = false
                    )
                }
            } catch (error: Exception) {
                handleError(error)
                _state.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    fun loadEarlier() {
        val token = authStore.state.value.accessToken
        if (token.isNullOrBlank()) {
            requestLogin()
            return
        }
        val beforeId = nextBeforeId ?: return
        if (!_state.value.hasMore || _state.value.isLoadingEarlier) return
        if (TestAccount.isLegacyToken(token)) {
            requestLogin()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingEarlier = true, errorMessage = null) }
            try {
                val result = apiClient.fetchHistory(roleId = role.id, beforeId = beforeId, token = token)
                nextBeforeId = result.nextBeforeId
                _state.update {
                    it.copy(
                        messages = result.messages + it.messages,
                        hasMore = result.hasMore,
                        isLoadingEarlier = false
                    )
                }
            } catch (error: Exception) {
                handleError(error)
                _state.update { it.copy(isLoadingEarlier = false) }
            }
        }
    }

    fun send(imageDataUrl: String? = null, localImageData: ByteArray? = null): Boolean {
        val text = _state.value.draft.trim()
        val image = imageDataUrl?.trim()
        if (_state.value.isSending) return false
        if (text.isEmpty() && image.isNullOrEmpty()) return false

        val token = authStore.state.value.accessToken
        if (token.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = APIError.MissingToken.message) }
            requestLogin()
            return false
        }
        if (TestAccount.isLegacyToken(token)) {
            requestLogin()
            return false
        }

        val userId = makeTemporaryId()
        val assistantId = makeTemporaryId()
        pendingUserMessageId = userId
        pendingAssistantMessageId = assistantId

        val userMessage = ChatMessage(
            id = userId,
            sender = MessageSender.User,
            content = text,
            createdAt = null,
            localImageData = localImageData
        )
        val assistantMessage = ChatMessage(
            id = assistantId,
            sender = MessageSender.Assistant,
            content = "",
            createdAt = null
        )

        _state.update {
            it.copy(
                draft = "",
                errorMessage = null,
                isSending = true,
                messages = it.messages + userMessage + assistantMessage
            )
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            var hadError = false
            try {
                apiClient.streamChat(
                    roleId = role.id,
                    message = text.ifBlank { null },
                    imageDataUrl = image?.ifBlank { null },
                    token = token
                ).collect { event ->
                    handleStreamEvent(event)
                    if (event is ChatStreamEvent.Failure) {
                        hadError = true
                    }
                }
            } catch (error: Exception) {
                hadError = true
                handleError(error)
            } finally {
                streamJob = null
                _state.update { it.copy(isSending = false) }
                fillEmptyAssistantIfNeeded(hadError)
            }
        }
        return true
    }

    fun clearHistory() {
        val token = authStore.state.value.accessToken
        if (token.isNullOrBlank()) {
            requestLogin()
            return
        }
        if (TestAccount.isLegacyToken(token)) {
            requestLogin()
            return
        }

        viewModelScope.launch {
            try {
                apiClient.clearHistory(roleId = role.id, token = token)
                nextBeforeId = null
                _state.update { it.copy(messages = emptyList(), hasMore = false, errorMessage = null) }
            } catch (error: Exception) {
                handleError(error)
            }
        }
    }

    fun cancelStream() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isSending = false) }
        val assistantId = pendingAssistantMessageId
        if (assistantId != null) {
            _state.update { current ->
                current.copy(messages = current.messages.map { message ->
                    if (message.id == assistantId && message.content.isEmpty()) {
                        message.copy(content = "已停止回复。")
                    } else {
                        message
                    }
                })
            }
        }
    }

    override fun onCleared() {
        cancelStream()
        super.onCleared()
    }

    private fun handleStreamEvent(event: ChatStreamEvent) {
        when (event) {
            is ChatStreamEvent.Start -> replaceMessage(pendingUserMessageId, event.event.userMessage)
            is ChatStreamEvent.Delta -> appendAssistantDelta(event.content)
            is ChatStreamEvent.Done -> {
                replaceMessage(pendingAssistantMessageId, event.event.assistantMessage)
                _state.update { it.copy(isSending = false) }
            }
            is ChatStreamEvent.Failure -> {
                _state.update { it.copy(errorMessage = event.error.message, isSending = false) }
                if (event.error.requiresLogin) {
                    requestLogin()
                }
            }
        }
    }

    private fun appendAssistantDelta(content: String) {
        val assistantId = pendingAssistantMessageId ?: return
        _state.update { current ->
            current.copy(messages = current.messages.map { message ->
                if (message.id == assistantId) {
                    message.copy(content = message.content + content)
                } else {
                    message
                }
            })
        }
    }

    private fun replaceMessage(id: Int?, replacement: ChatMessage) {
        if (id == null) {
            _state.update { it.copy(messages = it.messages + replacement) }
            return
        }

        _state.update { current ->
            val index = current.messages.indexOfFirst { it.id == id }
            if (index < 0) {
                current.copy(messages = current.messages + replacement)
            } else {
                val old = current.messages[index]
                val newMessage = if (replacement.localImageData == null && old.localImageData != null) {
                    replacement.copy(localImageData = old.localImageData)
                } else {
                    replacement
                }
                current.copy(messages = current.messages.toMutableList().also { it[index] = newMessage })
            }
        }
    }

    private fun fillEmptyAssistantIfNeeded(hadError: Boolean) {
        if (hadError) return
        val assistantId = pendingAssistantMessageId ?: return
        _state.update { current ->
            current.copy(messages = current.messages.map { message ->
                if (message.id == assistantId && message.content.isEmpty()) {
                    message.copy(content = "我刚刚没有收到完整回复，请再试一次。")
                } else {
                    message
                }
            })
        }
    }

    private fun handleError(error: Exception) {
        val apiError = error as? APIError ?: APIError.Transport(error.localizedMessage ?: "请求失败，请稍后重试")
        _state.update { it.copy(errorMessage = apiError.message) }
        if (apiError.requiresLogin) {
            requestLogin()
        }
    }

    private fun requestLogin() {
        viewModelScope.launch {
            authStore.clear()
            _effects.emit(ChatEffect.AuthExpired)
        }
    }

    private fun makeTemporaryId(): Int {
        val id = nextTemporaryId
        nextTemporaryId -= 1
        return id
    }
}
