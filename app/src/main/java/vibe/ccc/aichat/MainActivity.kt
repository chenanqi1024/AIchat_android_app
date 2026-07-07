package vibe.ccc.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vibe.ccc.aichat.ui.app.AIchatApp
import vibe.ccc.aichat.ui.theme.AIchatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIchatTheme {
                AIchatApp()
            }
        }
    }
}
