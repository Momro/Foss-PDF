package dev.momro.fosspdf.ui.viewer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.momro.fosspdf.R
import dev.momro.fosspdf.data.RecentRepository
import dev.momro.fosspdf.pdf.PdfViewerViewModel
import dev.momro.fosspdf.pdf.TextRun
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BASE_RENDER_SCALE = 2f

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    uri: Uri,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val vm: PdfViewerViewModel = viewModel(
        key = uri.toString(),
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PdfViewerViewModel(appContext, uri) as T
        },
    )
    val recents = remember { RecentRepository(appContext) }
    val scope = rememberCoroutineScope()

    var chromeVisible by remember { mutableStateOf(true) }
    var renameDialog by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectionPage by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(pageCount = { vm.pageCount })

    LaunchedEffect(uri) {
        recents.add(uri.toString(), vm.fileName.ifBlank { uri.lastPathSegment ?: "PDF" })
    }

    LaunchedEffect(pagerState.currentPage) {
        vm.currentPage = pagerState.currentPage
    }

    LaunchedEffect(vm.currentPage) {
        val target = vm.currentPage
        if (target in 0 until vm.pageCount) {
            if (selectionMode) {
                if (target != selectionPage) selectionPage = target
            } else if (target != pagerState.currentPage) {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    val currentPage = if (selectionMode) {
        selectionPage.coerceIn(0, (vm.pageCount - 1).coerceAtLeast(0))
    } else {
        pagerState.currentPage
    }

    val showChrome = chromeVisible || vm.loadError

    Scaffold(
        topBar = {
            if (showChrome) {
                Column {
                    TopAppBar(
                        title = {
                            Text(vm.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectionMode = !selectionMode
                                if (selectionMode) selectionPage = pagerState.currentPage
                            }) {
                                Icon(
                                    Icons.Filled.SelectAll,
                                    contentDescription = stringResource(R.string.select_text),
                                )
                            }
                            IconButton(onClick = {
                                if (vm.searchActive) vm.closeSearch() else vm.searchActive = true
                            }) {
                                Icon(
                                    if (vm.searchActive) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                            IconButton(onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        vm.shareIntent(),
                                        context.getString(R.string.share_pdf),
                                    )
                                )
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                            }
                            IconButton(onClick = { renameDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                            }
                        },
                    )
                    if (vm.searchActive) {
                        SearchBar(vm = vm)
                    }
                }
            }
        },
        bottomBar = {
            if (showChrome && vm.pageCount > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (selectionMode) {
                        Text(
                            text = stringResource(R.string.select_text_hint),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.page_indicator,
                            currentPage + 1,
                            vm.pageCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (vm.pageCount > 1) {
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { value ->
                                if (selectionMode) {
                                    selectionPage = value.toInt()
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(value.toInt()) }
                                }
                            },
                            valueRange = 0f..(vm.pageCount - 1).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                vm.loading -> CircularProgressIndicator()
                vm.loadError || vm.pageCount == 0 -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(stringResource(R.string.error_open_pdf))
                    if (vm.loadErrorDetail.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                vm.loadErrorDetail,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
                else -> if (selectionMode) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PageItem(
                            vm = vm,
                            page = currentPage,
                            selectionMode = true,
                            onSingleTap = { chromeVisible = !chromeVisible },
                        )
                    }
                } else {
                    HorizontalPager(state = pagerState) { page ->
                        PageItem(
                            vm = vm,
                            page = page,
                            selectionMode = false,
                            onSingleTap = { chromeVisible = !chromeVisible },
                        )
                    }
                }
            }
        }
    }

    if (renameDialog) {
        RenameDialog(
            initialName = vm.fileName.removeSuffix(".pdf"),
            onDismiss = { renameDialog = false },
            onConfirm = { newName ->
                vm.rename(newName) { success, name ->
                    renameDialog = false
                    if (success) {
                        scope.launch { recents.rename(uri.toString(), name) }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_rename),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun PageItem(
    vm: PdfViewerViewModel,
    page: Int,
    selectionMode: Boolean,
    onSingleTap: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val rawSize = vm.pageSizes.getOrNull(page) ?: (595f to 842f)
        val pageSize = (rawSize.first.takeIf { it > 0f } ?: 595f) to (rawSize.second.takeIf { it > 0f } ?: 842f)
        val fit = minOf(maxWidth.value / pageSize.first, maxHeight.value / pageSize.second)
            .coerceAtLeast(0.1f)
        val renderScale = fit * BASE_RENDER_SCALE
        val searchVersion = vm.hits.size
        var bitmap by remember(page) { mutableStateOf<ImageBitmap?>(null) }
        var failed by remember(page) { mutableStateOf(false) }
        var textRuns by remember(page) { mutableStateOf<List<TextRun>>(emptyList()) }

        LaunchedEffect(page, searchVersion) {
            failed = false
            val result = vm.renderPage(page, renderScale, vm.highlightsForPage(page))
            bitmap = result
            if (result == null) failed = true
        }

        LaunchedEffect(page) {
            textRuns = vm.textRunsForPage(page)
        }

        ZoomablePage(
            bitmap = bitmap,
            failed = failed,
            textRuns = textRuns,
            pageWidth = pageSize.first,
            pageHeight = pageSize.second,
            errorDetail = if (failed) vm.lastRenderError else "",
            noTextHint = selectionMode,
            onSingleTap = onSingleTap,
        )
    }
}

@Composable
private fun SearchBar(vm: PdfViewerViewModel) {
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { value ->
                vm.query = value
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(400)
                    vm.runSearch(value)
                }
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        if (vm.searching) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (vm.query.isNotBlank()) {
            val current = if (vm.currentHitIndex >= 0) vm.currentHitIndex + 1 else 0
            Text(
                text = if (vm.hits.isEmpty()) {
                    stringResource(R.string.no_results)
                } else {
                    stringResource(R.string.hit_counter, current, vm.hits.size)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = { vm.prevHit() }) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = stringResource(R.string.previous_result),
            )
        }
        IconButton(onClick = { vm.nextHit() }) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.next_result),
            )
        }
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_pdf)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.new_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
