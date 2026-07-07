package vibe.ccc.aichat.ui.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import vibe.ccc.aichat.data.auth.AppPreferences
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.auth.SecureTokenStore
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.data.role.RoleRepository

class AppContainer(
    context: Context,
    appScope: CoroutineScope
) {
    private val applicationContext = context.applicationContext
    private val okHttpClient = OkHttpClient()

    val appPreferences = AppPreferences(applicationContext)
    val apiClient = APIClient(okHttpClient)
    val authStore = AuthStore(
        appPreferences = appPreferences,
        tokenStore = SecureTokenStore(applicationContext),
        scope = appScope
    )
    val roleRepository = RoleRepository(apiClient)
}
