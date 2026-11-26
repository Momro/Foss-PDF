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

class PdfViewerViewModel(
    private val appContext: Context,
    val uri: Uri,
) : ViewModel() {

    private var document: PDDocument? = null
    private var renderer: PDFRenderer? = null
    private val bitmapCache = LruCache<String, ImageBitmap>(6)

    var loading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf(false)
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
                appContext.contentResolver.openInputStream(uri).use { stream ->
                    val doc = PDDocument.load(checkNotNull(stream))
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadError = true
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
            } catch (e: Exception) {
                null
            }
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
                val result = searchDocument(doc, rawQuery.trim())
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

    private fun displayName(): String =
        DocumentFile.fromSingleUri(appContext, uri)?.name?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
            ?: "PDF"

    override fun onCleared() {
        try {
            document?.close()
        } catch (_: Exception) {
        }
    }
}
