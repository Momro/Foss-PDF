package dev.momro.fosspdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dev.momro.fosspdf.ui.home.HomeScreen
import dev.momro.fosspdf.ui.theme.FossPdfTheme
import dev.momro.fosspdf.ui.viewer.PdfViewerScreen
import java.io.File

class MainActivity : ComponentActivity() {

    private var externalUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        installCrashHandler()
        val crashReport = readCrashReport()
        val viewUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        val initialScreen: Screen = viewUri?.let { Screen.Viewer(it) } ?: Screen.Home
        setContent {
            FossPdfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (crashReport != null) {
                        CrashScreen(
                            report = crashReport,
                            onDismiss = {
                                File(cacheDir, CRASH_FILE).delete()
                                recreate()
                            },
                        )
                    } else {
                        var screen by mutableStateOf(initialScreen)
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            externalUri = intent.data
        }
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(cacheDir, CRASH_FILE).writeText(
                    buildString {
                        append(throwable.toString()).append('\n')
                        throwable.stackTrace.forEach { append("    at ").append(it.toString()).append('\n') }
                    }
                )
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun readCrashReport(): String? {
        val file = File(cacheDir, CRASH_FILE)
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val CRASH_FILE = "last_crash.txt"
    }
}

@Composable
private fun CrashScreen(report: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Foss PDF ist abgestürzt:", style = MaterialTheme.typography.titleLarge)
        Text(report, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Button(onClick = onDismiss) { Text("Meldung löschen & neu starten") }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Viewer(val uri: Uri) : Screen
}
