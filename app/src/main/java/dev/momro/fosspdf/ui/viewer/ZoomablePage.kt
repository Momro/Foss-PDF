package dev.momro.fosspdf.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momro.fosspdf.R
import dev.momro.fosspdf.pdf.TextRun
import kotlin.math.min
import kotlin.math.roundToInt

const val ZOOM_SCALE = 2.5f
private const val MAX_SCALE = 5f
private const val ZOOM_EPSILON = 1.01f

@Composable
fun ZoomablePage(
    bitmap: ImageBitmap?,
    failed: Boolean,
    textRuns: List<TextRun> = emptyList(),
    pageWidth: Float = 595f,
    pageHeight: Float = 842f,
    errorDetail: String = "",
    onZoomChanged: (zoomed: Boolean) -> Unit = {},
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale > ZOOM_EPSILON) {
        onZoomChanged(scale > ZOOM_EPSILON)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else ZOOM_SCALE
                        if (scale <= 1f) offset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val prevPositions = HashMap<PointerId, Offset>()
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val current = pressed.associate { it.id to it.position }
                            val common = current.keys.filter { it in prevPositions }
                            if (common.size >= 2) {
                                val p0 = prevPositions[common[0]]!!
                                val p1 = prevPositions[common[1]]!!
                                val c0 = current[common[0]]!!
                                val c1 = current[common[1]]!!
                                val prevDist = (p0 - p1).getDistance()
                                val dist = (c0 - c1).getDistance()
                                if (prevDist > 0f && dist > 0f) {
                                    val zoom = dist / prevDist
                                    val centroid = (c0 + c1) / 2f
                                    val pan = centroid - (p0 + p1) / 2f
                                    val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                                    val pivot = Offset(size.width / 2f, size.height / 2f)
                                    var newOffset =
                                        offset + (scale - newScale) * (centroid - pivot) + pan
                                    if (newScale <= 1f) {
                                        newOffset = Offset.Zero
                                    } else {
                                        val maxX = (newScale - 1f) * size.width / 2f
                                        val maxY = (newScale - 1f) * size.height / 2f
                                        newOffset = Offset(
                                            newOffset.x.coerceIn(-maxX, maxX),
                                            newOffset.y.coerceIn(-maxY, maxY),
                                        )
                                    }
                                    scale = newScale
                                    offset = newOffset
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            prevPositions.clear()
                            prevPositions.putAll(current)
                        } else if (pressed.size == 1) {
                            val change = pressed[0]
                            val prev = prevPositions[change.id]
                            if (scale > ZOOM_EPSILON && prev != null) {
                                offset += change.position - prev
                                change.consume()
                            }
                            prevPositions.clear()
                            prevPositions[change.id] = change.position
                        } else {
                            prevPositions.clear()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (textRuns.isNotEmpty()) {
                    SelectionContainer(modifier = Modifier.fillMaxSize()) {
                        PageTextLayer(
                            runs = textRuns,
                            pageWidth = pageWidth,
                            pageHeight = pageHeight,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        when {
            bitmap == null && failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(stringResource(R.string.error_render_page))
                if (errorDetail.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            errorDetail,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            bitmap == null -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun PageTextLayer(
    runs: List<TextRun>,
    pageWidth: Float,
    pageHeight: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val fit = minOf(maxWidth.value / pageWidth, maxHeight.value / pageHeight)
            .coerceAtLeast(0.1f)
        val dx = (maxWidth.value - pageWidth * fit) / 2f
        val dy = (maxHeight.value - pageHeight * fit) / 2f
        Box(modifier = Modifier.fillMaxSize()) {
            for (run in runs) {
                Text(
                    text = run.text,
                    modifier = Modifier.offset {
                        IntOffset(
                            (dx + run.x * fit).roundToInt(),
                            (dy + run.y * fit).roundToInt(),
                        )
                    },
                    color = Color.Transparent,
                    fontSize = with(density) { (run.height * fit).toSp() },
                    lineHeight = with(density) { (run.height * fit).toSp() },
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
