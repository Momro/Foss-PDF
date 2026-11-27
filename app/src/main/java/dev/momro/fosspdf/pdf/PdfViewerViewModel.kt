package dev.momro.fosspdf.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class PdfViewerViewModel(
    private val appContext: Context,
    val uri: Uri,
) : ViewModel() {

    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null
    private val bitmapCache = LruCache<String, ImageBitmap>(6)
    private val textRunsCache = HashMap<Int, List<TextRun>>()

    var loading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf(false)
        private set
    var loadErrorDetail by mutableStateOf("")
        private set
    var lastRenderError by mutableStateOf("")
        private set
    var pageCount by mutableStateOf(0)
        private set
    var pageSizes by mutableStateOf<List<Pair<Float, Float>>>(emptyList())
        private set
    var fileName by mutableStateOf("")
        private set

    var currentPage by mutableStateOf(0)

    var searchActive by mutableStateOf(false)
    var query by mutableStateOf("")
    var searching by mutableStateOf(false)
    val hits = mutableStateListOf<SearchHit>()
    var currentHitIndex by mutableStateOf(-1)

    init {
        fileName = displayName()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val input = openStream() ?: throw FileNotFoundException("cannot open $uri")
                input.use { stream ->
                    val doc = PDDocument.load(stream)
                    document = doc
                    renderer = PDFRenderer(doc)
                    val sizes = ArrayList<Pair<Float, Float>>(doc.numberOfPages)
                    for (i in 0 until doc.numberOfPages) {
                        val box = doc.getPage(i).cropBox
                        sizes.add(box.width to box.height)
                    }
                    withContext(Dispatchers.Main) {
                        pageSizes = sizes
                        pageCount = doc.numberOfPages
                        loading = false
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    loadError = true
                    loadErrorDetail = e.toString()
                    loading = false
                }
            }
        }
    }

    suspend fun renderPage(page: Int, scale: Float, highlights: List<PdfRect>): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val key = "$page|${"%.2f".format(scale)}|${if (highlights.isEmpty()) "none" else highlights.size}"
            if (highlights.isEmpty()) {
                bitmapCache.get(key)?.let { return@withContext it }
            }
            val doc = document ?: return@withContext null
            val r = renderer ?: return@withContext null
            try {
                val bitmap = r.renderImage(page, scale)
                if (highlights.isNotEmpty()) {
                    val canvas = Canvas(bitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(110, 255, 213, 79)
                        style = Paint.Style.FILL
                    }
                    for (rect in highlights) {
                        canvas.drawRect(
                            rect.left * scale,
                            rect.top * scale,
                            rect.right * scale,
                            rect.bottom * scale,
                            paint,
                        )
                    }
                }
                val image = bitmap.asImageBitmap()
                if (highlights.isEmpty()) bitmapCache.put(key, image)
                image
            } catch (e: Throwable) {
                lastRenderError = e.toString()
                null
            }
        }

    suspend fun textRunsForPage(page: Int): List<TextRun> = withContext(Dispatchers.IO) {
        synchronized(textRunsCache) { textRunsCache[page] }?.let { return@withContext it }
        val doc = document ?: return@withContext emptyList()
        val runs = try {
            extractTextRuns(doc, page)
        } catch (_: Throwable) {
            emptyList()
        }
        synchronized(textRunsCache) { textRunsCache[page] = runs }
        runs
    }

    private fun openStream(): InputStream? {
        try {
            appContext.contentResolver.openInputStream(uri)?.let { return it }
        } catch (_: Throwable) {
        }
        val path = uri.takeIf { it.scheme.equals("file", ignoreCase = true) }?.path
        if (path != null) {
            try {
                return FileInputStream(path)
            } catch (_: Throwable) {
            }
        }
        return null
    }

    fun runSearch(rawQuery: String) {
        query = rawQuery
        if (rawQuery.isBlank()) {
            hits.clear()
            currentHitIndex = -1
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            searching = true
            try {
                val doc = document ?: return@launch
                val result = try {
                    searchDocument(doc, rawQuery.trim())
                } catch (e: Throwable) {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    hits.clear()
                    hits.addAll(result)
                    currentHitIndex = if (result.isEmpty()) -1 else 0
                    if (result.isNotEmpty()) currentPage = result.first().pageIndex
                }
            } finally {
                withContext(Dispatchers.Main) { searching = false }
            }
        }
    }

    fun nextHit() = moveHit(1)

    fun prevHit() = moveHit(-1)

    private fun moveHit(delta: Int) {
        if (hits.isEmpty()) return
        currentHitIndex = (currentHitIndex + delta).mod(hits.size)
        currentPage = hits[currentHitIndex].pageIndex
    }

    fun highlightsForPage(page: Int): List<PdfRect> =
        hits.filter { it.pageIndex == page }.flatMap { it.rects }

    fun closeSearch() {
        searchActive = false
        query = ""
        hits.clear()
        currentHitIndex = -1
    }

    fun shareIntent(): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    fun rename(newName: String, onDone: (success: Boolean, name: String) -> Unit) {
        val target = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
        viewModelScope.launch(Dispatchers.IO) {
            val ok = try {
                DocumentFile.fromSingleUri(appContext, uri)?.renameTo(target) == true
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                if (ok) fileName = target
                onDone(ok, if (ok) target else fileName)
            }
        }
    }

    private fun displayName(): String = try {
        DocumentFile.fromSingleUri(appContext, uri)?.name?.takeIf { it.isNotBlank() }
            ?: fallbackName()
    } catch (_: Throwable) {
        fallbackName()
    }

    private fun fallbackName(): String =
        uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "PDF"

    override fun onCleared() {
        try {
            document?.close()
        } catch (_: Exception) {
        }
    }
}
