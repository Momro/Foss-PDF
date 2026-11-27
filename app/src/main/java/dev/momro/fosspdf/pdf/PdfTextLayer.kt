package dev.momro.fosspdf.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.StringWriter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TextSpan(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

data class TextRun(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

private class SpanStripper : PDFTextStripper() {
    val spans = mutableListOf<TextSpan>()

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        for (tp in textPositions) {
            if (tp.unicode.isBlank()) continue
            spans.add(
                TextSpan(
                    text = tp.unicode,
                    x = tp.xDirAdj,
                    y = tp.yDirAdj,
                    width = tp.widthDirAdj,
                    height = tp.heightDir,
                )
            )
        }
    }
}

fun buildTextRuns(spans: List<TextSpan>, lineTolerance: Float = 2f): List<TextRun> {
    val runs = mutableListOf<TextRun>()
    var line = mutableListOf<TextSpan>()
    var lineY = 0f

    fun flush() {
        if (line.isEmpty()) return
        val sorted = line.sortedBy { it.x }
        val sb = StringBuilder()
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        var prev: TextSpan? = null
        for (span in sorted) {
            if (prev != null) {
                val gap = span.x - (prev.x + prev.width)
                if (gap > prev.height * 0.25f) sb.append(' ')
            }
            sb.append(span.text)
            left = min(left, span.x)
            top = min(top, span.y)
            right = max(right, span.x + span.width)
            bottom = max(bottom, span.y + span.height)
            prev = span
        }
        val text = sb.toString().trim()
        if (text.isNotEmpty() && right > left && bottom > top) {
            runs.add(TextRun(text, left, top, right - left, bottom - top))
        }
        line = mutableListOf()
    }

    for (span in spans) {
        if (line.isEmpty()) {
            lineY = span.y
        } else if (abs(span.y - lineY) > lineTolerance) {
            flush()
            lineY = span.y
        }
        line.add(span)
    }
    flush()
    return runs.sortedBy { it.y }
}

fun extractTextRuns(document: PDDocument, pageIndex: Int): List<TextRun> {
    val stripper = SpanStripper()
    stripper.sortByPosition = true
    stripper.startPage = pageIndex + 1
    stripper.endPage = pageIndex + 1
    stripper.writeText(document, StringWriter())
    return buildTextRuns(stripper.spans)
}
