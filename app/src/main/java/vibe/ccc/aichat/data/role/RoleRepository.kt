package vibe.ccc.aichat.data.role

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.network.APIClient

class RoleRepository(private val apiClient: APIClient) {
    private val _roles = MutableStateFlow(RolePresentation.defaults)
    val roles: StateFlow<List<ChatRole>> = _roles

    suspend fun loadRoles(): List<ChatRole> {
        return try {
            RolePresentation.figmaOrdered(apiClient.fetchRoles()).also { _roles.value = it }
        } catch (error: Exception) {
            _roles.value = RolePresentation.defaults
            throw error
        }
    }

    fun findRole(roleId: Int): ChatRole? =
        _roles.value.firstOrNull { it.id == roleId }
            ?: RolePresentation.defaults.firstOrNull { it.id == roleId }
}
