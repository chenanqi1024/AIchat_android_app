package vibe.ccc.aichat.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.ui.chat.ChatScreen
import vibe.ccc.aichat.ui.home.HomeScreen
import vibe.ccc.aichat.ui.onboarding.OnboardingScreen
import vibe.ccc.aichat.ui.settings.SettingsScreen

@Composable
fun AIchatApp() {
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()
    val container = remember(context) { AppContainer(context, appScope) }
    val authState by container.authStore.state.collectAsState()
    val hasSeenOnboarding by container.appPreferences.hasSeenOnboarding.collectAsState(initial = false)
    val navController = rememberNavController()
    var pendingLaunchRoleId by remember { mutableStateOf<Int?>(null) }

    if (!hasSeenOnboarding) {
        OnboardingScreen(
            apiClient = container.apiClient,
            roleRepository = container.roleRepository,
            appPreferences = container.appPreferences,
            authStore = container.authStore,
            authState = authState,
            onFinish = { role ->
                appScope.launch {
                    role?.let {
                        container.appPreferences.setSelectedRoleId(it.id)
                        pendingLaunchRoleId = it.id
                    }
                    container.appPreferences.setHasSeenOnboarding(true)
                }
            }
        )
    } else {
        LaunchedEffect(pendingLaunchRoleId) {
            pendingLaunchRoleId?.let { roleId ->
                navController.navigate(AppRoute.chat(roleId))
                pendingLaunchRoleId = null
            }
        }

        NavHost(
            navController = navController,
            startDestination = AppRoute.Home,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(AppRoute.Home) {
                HomeScreen(
                    apiClient = container.apiClient,
                    roleRepository = container.roleRepository,
                    appPreferences = container.appPreferences,
                    authStore = container.authStore,
                    authState = authState,
                    onOpenChat = { role: ChatRole ->
                        appScope.launch { container.appPreferences.setSelectedRoleId(role.id) }
                        navController.navigate(AppRoute.chat(role.id))
                    },
                    onOpenSettings = {
                        navController.navigate(AppRoute.Settings)
                    }
                )
            }
            composable(
                route = AppRoute.Chat,
                arguments = listOf(navArgument("roleId") { type = NavType.IntType })
            ) { backStackEntry ->
                val roleId = backStackEntry.arguments?.getInt("roleId") ?: 0
                val role = container.roleRepository.findRole(roleId)
                if (role == null) {
                    MissingRole()
                } else {
                    ChatScreen(
                        role = role,
                        apiClient = container.apiClient,
                        authStore = container.authStore,
                        authState = authState,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(AppRoute.Settings) {
                SettingsScreen(
                    apiClient = container.apiClient,
                    authStore = container.authStore,
                    authState = authState,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private object AppRoute {
    const val Home = "home"
    const val Settings = "settings"
    const val Chat = "chat/{roleId}"

    fun chat(roleId: Int): String = "chat/$roleId"
}

@Composable
private fun MissingRole() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("角色不存在，请返回重新选择")
    }
}
