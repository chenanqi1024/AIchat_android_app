package vibe.ccc.aichat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vibe.ccc.aichat.data.auth.AppPreferences
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.data.role.RoleRepository
import vibe.ccc.aichat.util.DateFormatters

data class RecentChatPreview(
    val role: ChatRole,
    val lastMessage: String,
    val timeText: String
)

data class RoleListUiState(
    val roles: List<ChatRole> = emptyList(),
    val recentChats: List<RecentChatPreview> = emptyList(),
    val selectedRoleId: Int = 0,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isLoadingRecentChats: Boolean = false
)

class RoleListViewModel(
    private val apiClient: APIClient,
    private val roleRepository: RoleRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(RoleListUiState(roles = roleRepository.roles.value))
    val state: StateFlow<RoleListUiState> = _state

    init {
        viewModelScope.launch {
            _state.update { it.copy(selectedRoleId = appPreferences.selectedRoleId.first()) }
            loadRoles()
        }
    }

    fun loadRoles() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val roles = roleRepository.loadRoles()
                ensureSelectedRole(roles)
                _state.update { it.copy(roles = roles, isLoading = false) }
            } catch (error: Exception) {
                val fallback = roleRepository.roles.value
                ensureSelectedRole(fallback)
                _state.update {
                    it.copy(
                        roles = fallback,
                        errorMessage = error.localizedMessage ?: "角色加载失败",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadRecentChats(token: String?) {
        val roles = _state.value.roles
        if (token.isNullOrBlank() || roles.isEmpty()) {
            _state.update { it.copy(recentChats = emptyList(), isLoadingRecentChats = false) }
            return
        }
        if (_state.value.isLoadingRecentChats) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingRecentChats = true) }
            val previews = mutableListOf<RecentChatPreview>()
            roles.forEach { role ->
                runCatching {
                    apiClient.fetchHistory(roleId = role.id, limit = 20, token = token)
                }.getOrNull()?.messages?.lastOrNull()?.let { message ->
                    previews += RecentChatPreview(
                        role = role,
                        lastMessage = message.content,
                        timeText = DateFormatters.relativeChatTime(message.createdAt)
                    )
                }
            }
            _state.update { it.copy(recentChats = previews, isLoadingRecentChats = false) }
        }
    }

    fun selectRole(role: ChatRole) {
        _state.update { it.copy(selectedRoleId = role.id) }
        viewModelScope.launch {
            appPreferences.setSelectedRoleId(role.id)
        }
    }

    private suspend fun ensureSelectedRole(roles: List<ChatRole>) {
        val current = _state.value.selectedRoleId
        if (current == 0 && roles.isNotEmpty()) {
            val firstId = roles.first().id
            appPreferences.setSelectedRoleId(firstId)
            _state.update { it.copy(selectedRoleId = firstId) }
        }
    }
}
