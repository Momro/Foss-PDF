package dev.momro.fosspdf.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.momro.fosspdf.R
import kotlin.math.sqrt

const val ZOOM_SCALE = 2.5f
private const val MAX_SCALE = 5f

@Composable
fun ZoomablePage(
    bitmap: ImageBitmap?,
    failed: Boolean,
    errorDetail: String = "",
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else ZOOM_SCALE
                        if (scale == 1f) offset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (scale > 1f) {
                        change.consume()
                        offset += dragAmount
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitPointerEventScope {
                        var prevDistance = -1f
                        var baseScale = 1f
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) break
                            val active = event.changes.filter { it.pressed }
                            if (active.size >= 2) {
                                val dx = active[0].position.x - active[1].position.x
                                val dy = active[0].position.y - active[1].position.y
                                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                if (prevDistance > 0f) {
                                    val newScale = (baseScale * distance / prevDistance).coerceIn(1f, MAX_SCALE)
                                    if (newScale > 1f) {
                                        scale = newScale
                                    } else {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                    active.forEach { it.consume() }
                                } else {
                                    baseScale = scale
                                }
                                prevDistance = distance
                            } else {
                                prevDistance = -1f
                            }
                            if (active.isEmpty()) break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
            failed -> Column(
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
            else -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}
