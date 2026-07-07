package vibe.ccc.aichat.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import vibe.ccc.aichat.data.auth.AppPreferences
import vibe.ccc.aichat.data.auth.AuthState
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.data.role.RoleRepository
import vibe.ccc.aichat.data.role.avatarImageUrl
import vibe.ccc.aichat.data.role.backgroundImageUrl
import vibe.ccc.aichat.data.role.displayName
import vibe.ccc.aichat.data.role.onboardingDescription
import vibe.ccc.aichat.ui.components.AppBrushes
import vibe.ccc.aichat.ui.components.GradientButton
import vibe.ccc.aichat.ui.components.RemoteImage
import vibe.ccc.aichat.ui.home.RoleListViewModel
import vibe.ccc.aichat.ui.login.LoginSheet
import vibe.ccc.aichat.ui.theme.AppPrimary
import vibe.ccc.aichat.ui.theme.Gray300
import vibe.ccc.aichat.ui.theme.Purple400
import vibe.ccc.aichat.ui.theme.TextMuted
import vibe.ccc.aichat.ui.theme.TextPrimary
import vibe.ccc.aichat.ui.theme.TextSecondary
import vibe.ccc.aichat.util.viewModelFactory

@Composable
fun OnboardingScreen(
    apiClient: APIClient,
    roleRepository: RoleRepository,
    appPreferences: AppPreferences,
    authStore: AuthStore,
    authState: AuthState,
    onFinish: (ChatRole?) -> Unit
) {
    val viewModel: RoleListViewModel = viewModel(
        factory = viewModelFactory { RoleListViewModel(apiClient, roleRepository, appPreferences) }
    )
    val state by viewModel.state.collectAsState()
    var isShowingLogin by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { state.roles.size.coerceAtLeast(1) })
    val selectedRole = state.roles.getOrNull(pagerState.currentPage) ?: state.roles.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBrushes.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Purple400)
                Text("欢迎来到陪伴世界", color = Purple400)
            }
            Spacer(Modifier.height(12.dp))
            Text("选择你的陪伴角色", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text("每位角色都有不同的性格与陪伴方式", color = TextMuted)
            Spacer(Modifier.height(32.dp))

            when {
                state.isLoading && state.roles.isEmpty() -> LoadingOnboardingCard("正在加载陪伴角色")
                state.errorMessage != null && state.roles.isEmpty() -> ErrorOnboardingCard(state.errorMessage ?: "角色加载失败", viewModel::loadRoles)
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                    ) { page ->
                        state.roles.getOrNull(page)?.let { role ->
                            OnboardingRoleCard(role)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PageDots(count = state.roles.size, selected = pagerState.currentPage)
            Spacer(Modifier.height(28.dp))
            GradientButton(
                text = "开始聊天",
                enabled = state.roles.isNotEmpty(),
                onClick = {
                    if (authState.isAuthenticated) {
                        onFinish(selectedRole)
                    } else {
                        isShowingLogin = true
                    }
                }
            )
        }
    }

    if (isShowingLogin) {
        LoginSheet(
            apiClient = apiClient,
            authStore = authStore,
            onDismiss = { isShowingLogin = false },
            onSuccess = { onFinish(selectedRole) }
        )
    }
}

@Composable
private fun OnboardingRoleCard(role: ChatRole) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Box {
            RemoteImage(url = role.backgroundImageUrl, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.64f)))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                RemoteImage(
                    url = role.avatarImageUrl,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.height(24.dp))
                Text(role.displayName, color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    role.onboardingDescription,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == selected) 32.dp else 8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) AppPrimary else Gray300)
            )
        }
    }
}

@Composable
private fun LoadingOnboardingCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().height(480.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(text, color = TextMuted)
        }
    }
}

@Composable
private fun ErrorOnboardingCard(message: String, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().height(480.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Text(message, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("重新加载")
            }
        }
    }
}
