package vibe.ccc.aichat.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.ui.components.AppBrushes
import vibe.ccc.aichat.ui.components.GradientButton
import vibe.ccc.aichat.ui.theme.AppWarm
import vibe.ccc.aichat.ui.theme.Gray100
import vibe.ccc.aichat.ui.theme.Purple400
import vibe.ccc.aichat.ui.theme.Purple600
import vibe.ccc.aichat.ui.theme.TextMuted
import vibe.ccc.aichat.ui.theme.TextPrimary
import vibe.ccc.aichat.util.viewModelFactory

@Composable
fun LoginSheet(
    apiClient: APIClient,
    authStore: AuthStore,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel(factory = viewModelFactory { LoginViewModel(apiClient) })
    val state by viewModel.state.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBrushes.purplePinkSoft)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Purple400, modifier = Modifier.size(20.dp))
                        Text("陪伴世界", color = Purple400, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text("欢迎回来", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
                    Text("使用手机号验证码登录", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = TextMuted)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text("手机号", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+86",
                        color = TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Gray100)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                    OutlinedTextField(
                        value = state.phoneNumber,
                        onValueChange = viewModel::updatePhoneNumber,
                        placeholder = { Text("请输入手机号") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("验证码", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.verifyCode,
                        onValueChange = viewModel::updateVerifyCode,
                        placeholder = { Text("请输入验证码") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = state.canSendCode,
                        onClick = viewModel::sendCode
                    ) {
                        Text(codeButtonTitle(state))
                    }
                }

                state.errorMessage?.let {
                    Text(it, color = AppWarm, style = MaterialTheme.typography.bodySmall)
                }

                GradientButton(
                    text = if (state.isLoggingIn) "登录中" else "登录",
                    enabled = state.canLogin,
                    onClick = {
                        viewModel.login(authStore) {
                            onSuccess()
                            onDismiss()
                        }
                    }
                )

                Text(
                    text = buildAnnotatedString {
                        append("登录即表示同意")
                        withStyle(SpanStyle(color = Purple600)) { append("《用户协议》") }
                        append("和")
                        withStyle(SpanStyle(color = Purple600)) { append("《隐私政策》") }
                    },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private fun codeButtonTitle(state: LoginUiState): String =
    when {
        state.isSendingCode -> "发送中"
        state.retryAfter > 0 -> "${state.retryAfter}秒"
        else -> "获取验证码"
    }
