package dev.momro.fosspdf.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.momro.fosspdf.R
import dev.momro.fosspdf.data.RecentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenPdf: (Uri) -> Unit) {
    val context = LocalContext.current
    val recents = remember { RecentRepository(context.applicationContext) }
    val recentList by recents.recents.collectAsState(initial = emptyList())

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            onOpenPdf(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.open_pdf))
            }
        },
    ) { padding ->
        if (recentList.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_recent_files))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Text(
                    text = stringResource(R.string.recently_opened),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                LazyColumn {
                    items(recentList, key = { it.uri }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            leadingContent = {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                )
                            },
                            modifier = Modifier.clickable { onOpenPdf(item.uri.toUri()) },
                        )
                    }
                }
            }
        }
    }
}
