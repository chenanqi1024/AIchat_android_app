package vibe.ccc.aichat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vibe.ccc.aichat.ui.theme.AppPink
import vibe.ccc.aichat.ui.theme.AppPrimary
import vibe.ccc.aichat.ui.theme.Blue100
import vibe.ccc.aichat.ui.theme.Gray100
import vibe.ccc.aichat.ui.theme.Pink100
import vibe.ccc.aichat.ui.theme.Purple100
import vibe.ccc.aichat.ui.theme.Purple50
import vibe.ccc.aichat.ui.theme.Pink50
import vibe.ccc.aichat.ui.theme.Blue50

object AppBrushes {
    val background = Brush.verticalGradient(listOf(Purple50, Pink50, Blue50))
    val hero = Brush.horizontalGradient(listOf(AppPrimary, AppPink))
    val soft = Brush.linearGradient(listOf(Purple100.copy(alpha = 0.60f), Pink100.copy(alpha = 0.42f), Blue100.copy(alpha = 0.60f)))
    val purplePinkSoft = Brush.linearGradient(listOf(Purple100, Pink100))
    val bluePurpleSoft = Brush.linearGradient(listOf(Blue100, Purple100))
    val imagePlaceholder = Brush.linearGradient(listOf(Gray100, Purple100))
}

@Composable
fun GradientButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppBrushes.hero else Brush.horizontalGradient(listOf(Gray100, Gray100)))
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0xFF9CA3AF),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 14.dp)
        )
    }
}

@Composable
fun RoundIconSurface(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.88f))
            .padding(10.dp),
        content = content
    )
}

@Composable
fun GradientScreenBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBrushes.background)
    ) {
        content()
    }
}
