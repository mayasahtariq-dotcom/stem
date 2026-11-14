package com.stemtutor.core.designsystem.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One shape of a sticker icon, in a 24×24 viewport. */
internal sealed interface IconShape {
    val fill: Long?
    val stroked: Boolean
}

/** A path; `fill = null` draws the outline only. */
internal data class P(val d: String, override val fill: Long?) : IconShape {
    override val stroked = true
    val path: Path by lazy { PathParser().parsePathString(d).toPath() }
}

internal data class C(
    val cx: Float,
    val cy: Float,
    val r: Float,
    override val fill: Long,
    override val stroked: Boolean,
) : IconShape

internal data class E(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    override val fill: Long,
    override val stroked: Boolean,
) : IconShape

private val Outline = Color(0xFF5A3D2E)
private const val VIEWPORT = 24f

/**
 * Mooshala's hand-drawn sticker icons: flat colour fills with a brown outline, used instead of
 * emoji everywhere. Decorative by default; pass [contentDescription] when the icon carries meaning.
 */
@Composable
fun StickerIcon(
    icon: Sticker,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val shapes = remember(icon) { StickerIconShapes[icon].orEmpty() }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.size(size).then(semantics)) {
        val k = this.size.minDimension / VIEWPORT
        scale(k, k, pivot = Offset.Zero) { shapes.forEach { drawIconShape(it) } }
    }
}

private val OutlineStroke = Stroke(width = 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawIconShape(shape: IconShape) {
    val fill = shape.fill?.let { Color(it) }
    when (shape) {
        is P -> {
            if (fill != null) drawPath(shape.path, fill)
            drawPath(shape.path, Outline, style = OutlineStroke)
        }
        is C -> {
            drawCircle(fill!!, shape.r, Offset(shape.cx, shape.cy))
            if (shape.stroked) drawCircle(Outline, shape.r, Offset(shape.cx, shape.cy), style = OutlineStroke)
        }
        is E -> {
            val topLeft = Offset(shape.cx - shape.rx, shape.cy - shape.ry)
            val size = Size(shape.rx * 2, shape.ry * 2)
            drawOval(fill!!, topLeft, size)
            if (shape.stroked) drawOval(Outline, topLeft, size, style = OutlineStroke)
        }
    }
}
