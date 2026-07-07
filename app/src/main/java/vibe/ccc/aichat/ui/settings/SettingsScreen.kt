package vibe.ccc.aichat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vibe.ccc.aichat.data.auth.AuthState
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.ui.components.AppBrushes
import vibe.ccc.aichat.ui.login.LoginSheet
import vibe.ccc.aichat.ui.theme.AppWarm
import vibe.ccc.aichat.ui.theme.Blue600
import vibe.ccc.aichat.ui.theme.Gray100
import vibe.ccc.aichat.ui.theme.Pink600
import vibe.ccc.aichat.ui.theme.Purple600
import vibe.ccc.aichat.ui.theme.TextMuted
import vibe.ccc.aichat.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    apiClient: APIClient,
    authStore: AuthStore,
    authState: AuthState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isShowingLogin by remember { mutableStateOf(false) }
    var isShowingLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(AppBrushes.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.88f))) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("设置", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                Text("账号与偏好", color = TextMuted)
            }
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Purple600, modifier = Modifier.size(34.dp))
        }

        AccountCard(authState)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)), shape = RoundedCornerShape(20.dp)) {
            Column {
                SettingsRow("消息通知", "保持默认提醒", Icons.Rounded.Notifications, Purple600)
                SettingsRow("隐私与安全", "账号安全状态正常", Icons.Rounded.Lock, Blue600)
                SettingsRow("关于陪伴世界", "AI 陪伴聊天 App", Icons.Rounded.Info, Pink600)
            }
        }

        if (authState.isAuthenticated) {
            Button(
                onClick = { isShowingLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("退出登录")
            }
        } else {
            Button(
                onClick = { isShowingLogin = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("登录账号")
            }
        }
    }

    if (isShowingLogin) {
        LoginSheet(
            apiClient = apiClient,
            authStore = authStore,
            onDismiss = { isShowingLogin = false },
            onSuccess = {}
        )
    }

    if (isShowingLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { isShowingLogoutConfirm = false },
            title = { Text("退出当前账号？") },
            text = { Text("退出后将无法同步最近聊天和历史记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingLogoutConfirm = false
                        scope.launch { authStore.clear() }
                    }
                ) {
                    Text("退出登录", color = AppWarm)
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingLogoutConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AccountCard(authState: AuthState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)), shape = RoundedCornerShape(24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AppBrushes.hero)
                    .padding(16.dp)
            )
            Column {
                Text(if (authState.isAuthenticated) "已登录" else "未登录", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(accountSubtitle(authState), color = TextMuted)
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Gray100).padding(10.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun accountSubtitle(authState: AuthState): String {
    if (!authState.isAuthenticated) return "登录后同步最近聊天"
    val user = authState.user ?: return "账号状态正常"
    return "${user.countryCode} ${user.phoneNumber}"
}
