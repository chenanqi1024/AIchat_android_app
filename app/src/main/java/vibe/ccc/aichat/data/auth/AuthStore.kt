package vibe.ccc.aichat.data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import vibe.ccc.aichat.data.model.AppUser
import vibe.ccc.aichat.data.model.LoginSession

data class AuthState(
    val accessToken: String? = null,
    val user: AppUser? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank()
}

class AuthStore(
    private val appPreferences: AppPreferences,
    private val tokenStore: SecureTokenStore,
    scope: CoroutineScope
) {
    private val accessToken = MutableStateFlow(tokenStore.read())

    val state: StateFlow<AuthState> = combine(accessToken, appPreferences.user) { token, user ->
        AuthState(accessToken = token, user = user)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AuthState(accessToken = accessToken.value)
    )

    suspend fun update(session: LoginSession) {
        tokenStore.save(session.accessToken)
        accessToken.value = session.accessToken
        appPreferences.persistUser(session.user)
    }

    suspend fun clear() {
        tokenStore.delete()
        accessToken.value = null
        appPreferences.clearUser()
    }
}
