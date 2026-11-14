package com.stemtutor.feature.notes.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.notes.CropRect
import com.stemtutor.feature.notes.PageDraft
import com.stemtutor.feature.notes.R
import kotlin.math.abs
import com.stemtutor.core.designsystem.R as DsR

/** Moves the corner nearest [touch] (in 0..1 units) to it, keeping a minimum crop size. */
fun dragCorner(crop: CropRect, touch: Offset): CropRect {
    val corners = listOf(Offset(crop.left, crop.top), Offset(crop.right, crop.top), Offset(crop.left, crop.bottom), Offset(crop.right, crop.bottom))
    val nearest = corners.indices.minBy { abs(corners[it].x - touch.x) + abs(corners[it].y - touch.y) }
    val x = touch.x.coerceIn(0f, 1f)
    val y = touch.y.coerceIn(0f, 1f)
    val min = CropRect.MIN_SIZE
    return when (nearest) {
        0 -> crop.copy(left = x.coerceAtMost(crop.right - min), top = y.coerceAtMost(crop.bottom - min))
        1 -> crop.copy(right = x.coerceAtLeast(crop.left + min), top = y.coerceAtMost(crop.bottom - min))
        2 -> crop.copy(left = x.coerceAtMost(crop.right - min), bottom = y.coerceAtLeast(crop.top + min))
        else -> crop.copy(right = x.coerceAtLeast(crop.left + min), bottom = y.coerceAtLeast(crop.top + min))
    }
}

/** Rotate in 90° steps and drag the corners to crop. Changes apply when Done is tapped. */
@Composable
fun CropRotateScreen(
    page: PageDraft,
    onRotate: () -> Unit,
    onDone: (CropRect) -> Unit,
    onBack: () -> Unit,
) {
    var crop by remember(page.id, page.rotation) { mutableStateOf(page.crop) }
    Column(Modifier.fillMaxSize().background(StemColors.Ink).statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            AsyncImage(
                model = page.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().rotate(page.rotation.toFloat()),
            )
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(page.id, page.rotation) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            crop = dragCorner(crop, Offset(change.position.x / size.width, change.position.y / size.height))
                        }
                    },
            ) {
                val l = crop.left * size.width
                val t = crop.top * size.height
                val r = crop.right * size.width
                val b = crop.bottom * size.height
                val shade = Color.Black.copy(alpha = 0.5f)
                drawRect(shade, Offset.Zero, Size(size.width, t))
                drawRect(shade, Offset(0f, b), Size(size.width, size.height - b))
                drawRect(shade, Offset(0f, t), Size(l, b - t))
                drawRect(shade, Offset(r, t), Size(size.width - r, b - t))
                drawRect(StemColors.Gold, Offset(l, t), Size(r - l, b - t), style = Stroke(4f))
                listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { drawCircle(StemColors.Gold, 18f, it) }
            }
        }
        Column(
            Modifier.fillMaxWidth().background(StemColors.White).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BilingualText(R.string.nt_crop_hint, size = BilingualTextSize.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(R.string.nt_rotate, onRotate, Modifier.weight(1f), ChunkyButtonStyle.White)
                ChunkyButton(DsR.string.ds_done, { onDone(crop) }, Modifier.weight(1f))
            }
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
}

@BilingualPreviews
@Composable
private fun CropRotatePreview() {
    StemTutorTheme {
        CropRotateScreen(PageDraft("1", com.stemtutor.core.data.model.PageKind.PHOTO, crop = CropRect(0.1f, 0.1f, 0.9f, 0.8f)), {}, {}, {})
    }
}
