package dev.momro.fosspdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.momro.fosspdf.ui.home.HomeScreen
import dev.momro.fosspdf.ui.theme.FossPdfTheme
import dev.momro.fosspdf.ui.viewer.PdfViewerScreen

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        setContent {
            FossPdfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by mutableStateOf<Screen>(Screen.Home)
                    LaunchedEffect(externalUri) {
                        externalUri?.let {
                            screen = Screen.Viewer(it)
                            externalUri = null
                        }
                    }
                    when (val current = screen) {
                        is Screen.Home -> HomeScreen(
                            onOpenPdf = { uri -> screen = Screen.Viewer(uri) },
                        )
                        is Screen.Viewer -> PdfViewerScreen(
                            uri = current.uri,
                            onBack = { screen = Screen.Home },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            externalUri = intent.data
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Viewer(val uri: Uri) : Screen
}
