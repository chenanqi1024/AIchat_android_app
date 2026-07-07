package vibe.ccc.aichat.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vibe.ccc.aichat.data.model.ChatMessage
import vibe.ccc.aichat.data.model.MessageSender
import vibe.ccc.aichat.ui.theme.TextAssistant

@Composable
fun MessageBubble(
    message: ChatMessage,
    assistantAvatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.User
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
    ) {
        if (!isUser) {
            RemoteImage(
                url = assistantAvatarUrl,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(0.76f)
        ) {
            message.localImageData?.let { ImageAttachment(it) }
            when {
                message.content.isEmpty() && !isUser && message.localImageData == null -> ThinkingBubble()
                message.content.isNotEmpty() -> TextBubble(message.content, isUser)
            }
        }
    }
}

@Composable
private fun TextBubble(content: String, isUser: Boolean) {
    Text(
        text = content,
        color = if (isUser) Color.White else TextAssistant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = if (isUser) TextAlign.End else TextAlign.Start,
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                )
            )
            .background(if (isUser) AppBrushes.hero else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.White, Color.White)))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun ThinkingBubble() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text("思考中", color = TextAssistant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ImageAttachment(data: ByteArray) {
    val bitmap = remember(data) {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(4.dp)
        )
    }
}
