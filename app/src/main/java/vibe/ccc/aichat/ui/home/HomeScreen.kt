package vibe.ccc.aichat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import vibe.ccc.aichat.data.auth.AuthState
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.data.role.RoleRepository
import vibe.ccc.aichat.data.role.avatarImageUrl
import vibe.ccc.aichat.data.role.displayName
import vibe.ccc.aichat.data.role.displayTag
import vibe.ccc.aichat.data.role.greeting
import vibe.ccc.aichat.data.role.homeDescription
import vibe.ccc.aichat.data.role.backgroundImageUrl
import vibe.ccc.aichat.data.auth.AppPreferences
import vibe.ccc.aichat.ui.components.AppBrushes
import vibe.ccc.aichat.ui.components.RemoteImage
import vibe.ccc.aichat.ui.login.LoginSheet
import vibe.ccc.aichat.ui.theme.AppPrimary
import vibe.ccc.aichat.ui.theme.Blue600
import vibe.ccc.aichat.ui.theme.Gray100
import vibe.ccc.aichat.ui.theme.Pink600
import vibe.ccc.aichat.ui.theme.Purple600
import vibe.ccc.aichat.ui.theme.TextMuted
import vibe.ccc.aichat.ui.theme.TextPrimary
import vibe.ccc.aichat.ui.theme.TextSecondary
import vibe.ccc.aichat.util.viewModelFactory

@Composable
fun HomeScreen(
    apiClient: APIClient,
    roleRepository: RoleRepository,
    appPreferences: AppPreferences,
    authStore: AuthStore,
    authState: AuthState,
    onOpenChat: (ChatRole) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: RoleListViewModel = viewModel(
        factory = viewModelFactory { RoleListViewModel(apiClient, roleRepository, appPreferences) }
    )
    val state by viewModel.state.collectAsState()
    var isShowingLogin by remember { mutableStateOf(false) }
    var pendingRole by remember { mutableStateOf<ChatRole?>(null) }

    LaunchedEffect(authState.accessToken, state.roles) {
        viewModel.loadRecentChats(authState.accessToken)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBrushes.background)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 80.dp)
        ) {
            HomeHeader(onOpenSettings = onOpenSettings)
            FeaturedSection(
                state = state,
                onRefresh = viewModel::loadRoles,
                onOpenRole = { role ->
                    viewModel.selectRole(role)
                    if (authState.isAuthenticated) {
                        onOpenChat(role)
                    } else {
                        pendingRole = role
                        isShowingLogin = true
                    }
                }
            )
            AllRolesSection(
                roles = state.roles,
                selectedRoleId = state.selectedRoleId,
                onRoleClick = { role ->
                    viewModel.selectRole(role)
                    if (authState.isAuthenticated) {
                        onOpenChat(role)
                    } else {
                        pendingRole = role
                        isShowingLogin = true
                    }
                }
            )
            QuickActionsSection(
                onContinue = {
                    val role = state.recentChats.firstOrNull()?.role ?: state.roles.firstOrNull { it.id == state.selectedRoleId } ?: state.roles.firstOrNull()
                    if (role != null) {
                        viewModel.selectRole(role)
                        if (authState.isAuthenticated) onOpenChat(role) else {
                            pendingRole = role
                            isShowingLogin = true
                        }
                    }
                },
                onRefresh = viewModel::loadRoles
            )
            RecentChatsSection(
                isAuthenticated = authState.isAuthenticated,
                isLoading = state.isLoadingRecentChats,
                chats = state.recentChats,
                onOpenChat = onOpenChat
            )
        }
    }

    if (isShowingLogin) {
        LoginSheet(
            apiClient = apiClient,
            authStore = authStore,
            onDismiss = { isShowingLogin = false },
            onSuccess = {
                pendingRole?.let(onOpenChat)
                pendingRole = null
            }
        )
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = AppPrimary)
                Text(greetingText(), color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            }
            Text("今天想和谁聊聊天？", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f))
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = Purple600)
        }
    }
}

@Composable
private fun FeaturedSection(
    state: RoleListUiState,
    onRefresh: () -> Unit,
    onOpenRole: (ChatRole) -> Unit
) {
    val featuredRole = state.roles.firstOrNull { it.id == state.selectedRoleId } ?: state.roles.firstOrNull()
    when {
        state.isLoading && state.roles.isEmpty() -> LoadingCard("正在加载角色")
        state.errorMessage != null && state.roles.isEmpty() -> ErrorCard(state.errorMessage, onRefresh)
        featuredRole != null -> FeaturedRoleCard(featuredRole, onOpenRole)
    }
}

@Composable
private fun FeaturedRoleCard(role: ChatRole, onOpenRole: (ChatRole) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            RemoteImage(url = role.backgroundImageUrl, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(AppBrushes.soft))
            Column(Modifier.padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    RemoteImage(
                        url = role.avatarImageUrl,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(role.displayName, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                            Text(
                                role.displayTag,
                                color = Purple600,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color.White.copy(alpha = 0.82f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(role.homeDescription, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(role.greeting, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onOpenRole(role) }, modifier = Modifier.fillMaxWidth()) {
                    Text("立即聊天")
                }
            }
        }
    }
}

@Composable
private fun AllRolesSection(
    roles: List<ChatRole>,
    selectedRoleId: Int,
    onRoleClick: (ChatRole) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("所有角色", color = TextPrimary, fontWeight = FontWeight.Medium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            roles.forEach { role ->
                RoleTile(role = role, selected = role.id == selectedRoleId, onClick = { onRoleClick(role) })
            }
        }
    }
}

@Composable
private fun RoleTile(role: ChatRole, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 10.dp else 5.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(128.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            RemoteImage(
                url = role.avatarImageUrl,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(role.displayName, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(
                role.homeDescription,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun QuickActionsSection(onContinue: () -> Unit, onRefresh: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction("继续聊天", Icons.Rounded.Message, Purple600, onContinue, Modifier.weight(1f))
        QuickAction("聊天记录", Icons.Rounded.History, Blue600, onContinue, Modifier.weight(1f))
        QuickAction("重新加载", Icons.Rounded.Refresh, Pink600, onRefresh, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun RecentChatsSection(
    isAuthenticated: Boolean,
    isLoading: Boolean,
    chats: List<RecentChatPreview>,
    onOpenChat: (ChatRole) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("最近聊天", color = TextPrimary, fontWeight = FontWeight.Medium)
        when {
            !isAuthenticated -> PlaceholderCard("登录后查看最近聊天")
            isLoading -> LoadingCard("正在同步聊天记录")
            chats.isEmpty() -> PlaceholderCard("暂无最近聊天")
            else -> chats.forEach { chat ->
                RecentChatRow(chat, onOpenChat)
            }
        }
    }
}

@Composable
private fun RecentChatRow(chat: RecentChatPreview, onOpenChat: (ChatRole) -> Unit) {
    Card(onClick = { onOpenChat(chat.role) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            RemoteImage(url = chat.role.avatarImageUrl, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(chat.role.displayName, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(chat.timeText, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(chat.lastMessage, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
private fun LoadingCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(text, color = TextMuted)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRefresh: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Text(message, color = TextSecondary)
            Button(onClick = onRefresh) { Text("重新加载") }
        }
    }
}

@Composable
private fun PlaceholderCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

private fun greetingText(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
}
