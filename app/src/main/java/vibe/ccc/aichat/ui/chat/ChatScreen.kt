package vibe.ccc.aichat.ui.chat

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vibe.ccc.aichat.data.auth.AuthState
import vibe.ccc.aichat.data.auth.AuthStore
import vibe.ccc.aichat.data.model.ChatRole
import vibe.ccc.aichat.data.network.APIClient
import vibe.ccc.aichat.data.role.avatarImageUrl
import vibe.ccc.aichat.data.role.backgroundImageUrl
import vibe.ccc.aichat.data.role.chatTag
import vibe.ccc.aichat.data.role.displayName
import vibe.ccc.aichat.data.role.welcomeMessage
import vibe.ccc.aichat.ui.components.AppBrushes
import vibe.ccc.aichat.ui.components.MessageBubble
import vibe.ccc.aichat.ui.components.RemoteImage
import vibe.ccc.aichat.ui.login.LoginSheet
import vibe.ccc.aichat.ui.theme.AppWarm
import vibe.ccc.aichat.ui.theme.Gray100
import vibe.ccc.aichat.ui.theme.TextMuted
import vibe.ccc.aichat.ui.theme.TextPrimary
import vibe.ccc.aichat.util.CompressedImageAttachment
import vibe.ccc.aichat.util.ImageCompressor
import vibe.ccc.aichat.util.viewModelFactory

@Composable
fun ChatScreen(
    role: ChatRole,
    apiClient: APIClient,
    authStore: AuthStore,
    authState: AuthState,
    onBack: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel(
        key = "chat-${role.id}",
        factory = viewModelFactory { ChatViewModel(role, apiClient, authStore) }
    )
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isShowingLogin by remember { mutableStateOf(false) }
    var isShowingClearConfirm by remember { mutableStateOf(false) }
    var selectedImageData by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageDataUrl by remember { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var imageErrorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isProcessingImage = true
            imageErrorMessage = null
            selectedImageData = null
            selectedImageDataUrl = null
            try {
                val attachment: CompressedImageAttachment = withContext(Dispatchers.Default) {
                    ImageCompressor.compress(context.contentResolver, uri)
                }
                selectedImageData = attachment.data
                selectedImageDataUrl = attachment.dataUrl
            } catch (error: Exception) {
                imageErrorMessage = error.localizedMessage ?: "图片压缩失败，请换一张图片试试"
            } finally {
                isProcessingImage = false
            }
        }
    }

    LaunchedEffect(authState.accessToken, role.id) {
        if (authState.isAuthenticated) {
            viewModel.loadHistory()
        } else {
            isShowingLogin = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            if (effect is ChatEffect.AuthExpired) {
                isShowingLogin = true
            }
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.cancelStream() }
    }

    Box(Modifier.fillMaxSize().background(AppBrushes.background)) {
        RemoteImage(url = role.backgroundImageUrl, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.78f)))

        Column(Modifier.fillMaxSize()) {
            ChatTopBar(role = role, onBack = onBack, onClear = { isShowingClearConfirm = true })
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (state.hasMore) {
                    item {
                        TextButton(onClick = viewModel::loadEarlier, modifier = Modifier.fillMaxWidth()) {
                            if (state.isLoadingEarlier) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("加载更早消息")
                            }
                        }
                    }
                }

                if (state.isLoadingHistory) {
                    item {
                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 80.dp)) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(8.dp))
                            Text("加载聊天记录", color = TextMuted)
                        }
                    }
                } else if (state.messages.isEmpty()) {
                    item {
                        WelcomeBubble(role)
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message, assistantAvatarUrl = role.avatarImageUrl)
                }
            }
            QuickTopics { viewModel.updateDraft(it) }
            Composer(
                draft = state.draft,
                isSending = state.isSending,
                canSend = !isProcessingImage && (state.draft.trim().isNotEmpty() || selectedImageDataUrl != null),
                errorMessage = state.errorMessage,
                imageErrorMessage = imageErrorMessage,
                selectedImageData = selectedImageData,
                isProcessingImage = isProcessingImage,
                onDraftChange = viewModel::updateDraft,
                onPickImage = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearImage = {
                    selectedImageData = null
                    selectedImageDataUrl = null
                    imageErrorMessage = null
                },
                onSend = {
                    if (state.isSending) {
                        viewModel.cancelStream()
                    } else {
                        val didSend = viewModel.send(
                            imageDataUrl = selectedImageDataUrl,
                            localImageData = selectedImageData
                        )
                        if (didSend) {
                            selectedImageData = null
                            selectedImageDataUrl = null
                            imageErrorMessage = null
                        }
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
            onSuccess = { viewModel.loadHistory() }
        )
    }

    if (isShowingClearConfirm) {
        AlertDialog(
            onDismissRequest = { isShowingClearConfirm = false },
            title = { Text("清空与 ${role.displayName} 的聊天记录？") },
            text = { Text("该操作只会清空当前账号与此角色的聊天历史。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingClearConfirm = false
                        viewModel.clearHistory()
                    }
                ) {
                    Text("清空聊天", color = AppWarm)
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ChatTopBar(role: ChatRole, onBack: () -> Unit, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.90f))
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Gray100)) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = TextPrimary)
        }
        RemoteImage(url = role.avatarImageUrl, modifier = Modifier.size(40.dp).clip(CircleShape))
        Column(Modifier.weight(1f)) {
            Text(role.displayName, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(role.chatTag, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onClear, modifier = Modifier.clip(CircleShape).background(Gray100)) {
            Icon(Icons.Rounded.Delete, contentDescription = "清空聊天", tint = TextPrimary)
        }
    }
}

@Composable
private fun WelcomeBubble(role: ChatRole) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        RemoteImage(url = role.avatarImageUrl, modifier = Modifier.size(32.dp).clip(CircleShape))
        Text(
            text = role.welcomeMessage,
            color = TextPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(0.78f)
        )
    }
}

@Composable
private fun QuickTopics(onSelect: (String) -> Unit) {
    val topics = listOf("今天有点累", "安慰我一下", "陪我聊聊天", "听我说说话")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        topics.forEach { topic ->
            Button(onClick = { onSelect(topic) }) {
                Text(topic)
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    isSending: Boolean,
    canSend: Boolean,
    errorMessage: String?,
    imageErrorMessage: String?,
    selectedImageData: ByteArray?,
    isProcessingImage: Boolean,
    onDraftChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.86f))
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        errorMessage?.let { Text(it, color = AppWarm, style = MaterialTheme.typography.bodySmall) }
        imageErrorMessage?.let { Text(it, color = AppWarm, style = MaterialTheme.typography.bodySmall) }
        if (selectedImageData != null || isProcessingImage) {
            ImagePreview(selectedImageData, isProcessingImage, onClearImage)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(
                enabled = !isSending && !isProcessingImage,
                onClick = onPickImage,
                modifier = Modifier.clip(CircleShape).background(Gray100)
            ) {
                Icon(Icons.Rounded.Photo, contentDescription = "选择图片", tint = TextPrimary)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("说点什么...") },
                minLines = 1,
                maxLines = 5,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = isSending || canSend,
                onClick = onSend,
                modifier = Modifier.clip(CircleShape).background(if (isSending || canSend) AppBrushes.hero else AppBrushes.imagePlaceholder)
            ) {
                Icon(
                    imageVector = if (isSending) Icons.Rounded.Stop else Icons.Rounded.Send,
                    contentDescription = if (isSending) "停止" else "发送",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ImagePreview(data: ByteArray?, processing: Boolean, onClear: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.90f))
            .padding(10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Gray100)
        ) {
            val bitmap = remember(data) {
                data?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
            }
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        Text(if (processing) "正在处理图片" else "图片已添加", color = TextPrimary, modifier = Modifier.weight(1f))
        if (data != null) {
            IconButton(onClick = onClear, modifier = Modifier.clip(CircleShape).background(Gray100)) {
                Icon(Icons.Rounded.Close, contentDescription = "移除图片", tint = TextMuted)
            }
        }
    }
}
