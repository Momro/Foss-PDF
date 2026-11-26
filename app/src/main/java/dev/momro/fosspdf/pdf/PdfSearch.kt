package dev.momro.fosspdf.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.StringWriter
import kotlin.math.roundToInt

data class PdfRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class SearchHit(
    val pageIndex: Int,
    val snippet: String,
    val rects: List<PdfRect>,
)

class PositionStripper : PDFTextStripper() {
    private val raw = StringBuilder()
    private val positions = mutableListOf<TextPosition?>()

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        raw.append(text)
        textPositions.forEach { positions.add(it) }
    }

    override fun writeLineSeparator() {
        raw.append('\n')
        positions.add(null)
    }

    override fun writeWordSeparator() {
        raw.append(' ')
        positions.add(null)
    }

    fun normalized(): Pair<String, List<TextPosition?>> {
        val out = StringBuilder()
        val map = mutableListOf<TextPosition?>()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c.isWhitespace()) {
                if (out.isNotEmpty() && out.last() != ' ') {
                    out.append(' ')
                    map.add(null)
                }
                i++
            } else {
                out.append(c)
                map.add(positions.getOrNull(i))
                i++
            }
        }
        return out.toString() to map
    }
}

fun searchDocument(document: PDDocument, query: String, maxHits: Int = 500): List<SearchHit> {
    val hits = mutableListOf<SearchHit>()
    val q = query.trim()
    if (q.isEmpty()) return hits
    for (pageIndex in 0 until document.numberOfPages) {
        val stripper = PositionStripper()
        stripper.sortByPosition = true
        stripper.startPage = pageIndex + 1
        stripper.endPage = pageIndex + 1
        stripper.writeText(document, StringWriter())
        val (text, map) = stripper.normalized()
        for (start in findMatchIndices(text, q)) {
            val end = start + q.length
            val byLine = mutableMapOf<Int, MutableList<TextPosition>>()
            for (i in start until end) {
                val tp = map.getOrNull(i) ?: continue
                val lineKey = (tp.yDirAdj / 2f).roundToInt()
                byLine.getOrPut(lineKey) { mutableListOf() }.add(tp)
            }
            val rects = byLine.values.map { line ->
                PdfRect(
                    left = line.minOf { it.xDirAdj },
                    top = line.minOf { it.yDirAdj },
                    right = line.maxOf { it.xDirAdj + it.widthDirAdj },
                    bottom = line.maxOf { it.yDirAdj + it.heightDir },
                )
            }
            val from = maxOf(0, start - 24)
            val to = minOf(text.length, end + 24)
            hits.add(SearchHit(pageIndex, text.substring(from, to).trim(), rects))
            if (hits.size >= maxHits) return hits
        }
    }
    return hits
}
