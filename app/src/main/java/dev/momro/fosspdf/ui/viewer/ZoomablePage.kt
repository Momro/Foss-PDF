package dev.momro.fosspdf.ui.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.momro.fosspdf.R

const val ZOOM_SCALE = 2.5f

@Composable
fun ZoomablePage(
    bitmap: ImageBitmap?,
    failed: Boolean,
    zoomed: Boolean,
    onZoomToggle: () -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scale = if (zoomed) ZOOM_SCALE else 1f

    LaunchedEffect(zoomed) {
        if (!zoomed) offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { onZoomToggle() },
                )
            }
            .pointerInput(zoomed) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (zoomed) offset += dragAmount
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
            failed -> Text(stringResource(R.string.error_render_page))
            else -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
        }
    }
}
