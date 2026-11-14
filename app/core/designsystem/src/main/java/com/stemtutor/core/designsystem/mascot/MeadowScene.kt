package com.stemtutor.core.designsystem.mascot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sin

private const val SCENE_W = 400f
private const val SCENE_H = 300f

/**
 * The home-screen meadow: sky, turning sun, drifting clouds, hills, barn, trees, fence, pond,
 * flowers and butterflies. Fills its bounds like SVG `xMidYMax slice`; [content] is laid on top
 * (usually the cow).
 */
@Composable
fun MeadowScene(
    modifier: Modifier = Modifier,
    height: Dp = 280.dp,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val phase: () -> Float = if (animate) {
        val t = rememberInfiniteTransition(label = "meadow")
        val p = t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
            label = "meadowPhase",
        )
        ({ p.value })
    } else {
        ({ 0f })
    }
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val k = max(size.width / SCENE_W, size.height / SCENE_H)
            val dx = (size.width - SCENE_W * k) / 2f
            val dy = size.height - SCENE_H * k
            withTransform({
                translate(dx, dy)
                scale(k, k, pivot = Offset.Zero)
            }) { drawMeadow(phase()) }
        }
        content()
    }
}

private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

private object Scene {
    val hillBack by lazy { p("M0 150Q60 104 130 136T260 128T400 132V300H0Z") }
    val hillMid by lazy { p("M0 176Q90 136 190 168T400 160V300H0Z") }
    val hillFront by lazy { p("M0 214Q200 190 400 214V300H0Z") }
    val roof by lazy { p("M-6 24L31 -4L68 24Z") }
    val doorX by lazy { p("M21 38L41 68M41 38L21 68") }
}

private val skyBrush = Brush.verticalGradient(0f to Color(0xFF8FD3F4), 1f to Color(0xFFE3F6FD), startY = 0f, endY = 300f)
private val flowers = listOf(
    Triple(30f, 262f, 0xFFF7A1B8), Triple(70f, 250f, 0xFFFFD25A), Triple(118f, 276f, 0xFFFFFFFF),
    Triple(170f, 258f, 0xFFF7A1B8), Triple(236f, 272f, 0xFFFFD25A), Triple(280f, 256f, 0xFFFFFFFF),
    Triple(330f, 270f, 0xFFF7A1B8), Triple(372f, 254f, 0xFFFFD25A), Triple(210f, 286f, 0xFFFFFFFF),
    Triple(150f, 290f, 0xFFFFD25A),
)
private val tufts = listOf(20f to 236f, 130f to 240f, 300f to 236f, 360f to 288f, 250f to 296f, 190f to 232f)

private fun DrawScope.drawMeadow(t: Float) {
    drawRect(skyBrush, size = Size(SCENE_W, SCENE_H))

    // Sun with slowly turning rays.
    rotate(t * 720f, pivot = Offset(330f, 52f)) {
        for (a in 0 until 6) {
            rotate(a * 30f, pivot = Offset(330f, 52f)) {
                drawRoundRect(Color(0xFFFFE08A), Offset(326f, 12f), Size(8f, 80f), CornerRadius(4f), alpha = .55f)
            }
        }
    }
    drawCircle(Color(0xFFFFD25A), 24f, Offset(330f, 52f))

    // Clouds drifting left to right.
    cloud(-140f + ((t * 2.3f) % 1f) * 580f, 0f, 1f)
    cloud(-140f + ((t * 1.6f + .4f) % 1f) * 580f - 150f, -18f, .7f)

    drawPath(Scene.hillBack, Color(0xFFBFE39A))
    drawPath(Scene.hillMid, Color(0xFF9FD477))

    // Barn.
    translate(286f, 132f) {
        drawRect(Color(0xFFD9534F), Offset(0f, 22f), Size(62f, 46f))
        drawRect(Color(0xFF8A3B36), Offset(0f, 22f), Size(62f, 46f), style = Stroke(3f))
        drawPath(Scene.roof, Color(0xFF9C3F3A))
        drawPath(Scene.roof, Color(0xFF6E2B27), style = Stroke(3f))
        drawRect(Color(0xFFFFF4E6), Offset(21f, 38f), Size(20f, 30f))
        drawRect(Color(0xFF8A3B36), Offset(21f, 38f), Size(20f, 30f), style = Stroke(3f))
        drawPath(Scene.doorX, Color(0xFF8A3B36), style = Stroke(2.5f))
        drawCircle(Color(0xFFFFF4E6), 5f, Offset(31f, 12f))
    }

    // Trees.
    drawRoundRect(Color(0xFF8A6650), Offset(34f, 140f), Size(10f, 56f), CornerRadius(3f))
    drawCircle(Color(0xFF3C9A55), 30f, Offset(39f, 128f))
    drawCircle(Color(0xFF4FAE5F), 18f, Offset(20f, 144f))
    drawCircle(Color(0xFF4FAE5F), 18f, Offset(58f, 146f))
    drawCircle(Color(0xFFE2574C), 5f, Offset(30f, 120f))
    drawCircle(Color(0xFFE2574C), 5f, Offset(50f, 138f))
    drawRoundRect(Color(0xFF8A6650), Offset(372f, 160f), Size(8f, 40f), CornerRadius(3f))
    drawCircle(Color(0xFF5BB04F), 22f, Offset(376f, 150f))

    drawPath(Scene.hillFront, Color(0xFF7CBF5A))

    // Fence.
    for (x in listOf(100f, 140f, 180f, 220f, 260f)) {
        drawLine(Color(0xFFB58A5A), Offset(x, 214f), Offset(x, 186f), 5f, StrokeCap.Round)
    }
    drawLine(Color(0xFFB58A5A), Offset(96f, 194f), Offset(264f, 194f), 4f, StrokeCap.Round)
    drawLine(Color(0xFFB58A5A), Offset(96f, 206f), Offset(264f, 206f), 4f, StrokeCap.Round)

    // Pond with a breathing ripple and a lily pad.
    drawOval(Color(0xFF7CC8EC), Offset(14f, 248f), Size(104f, 28f))
    drawOval(Color(0xFF5AAEE8), Offset(14f, 248f), Size(104f, 28f), style = Stroke(3f))
    val rip = 1f + sin(t * 188f) * .08f
    scale(rip, rip * 1.1f, pivot = Offset(66f, 262f)) {
        drawOval(Color.White, Offset(36f, 256f), Size(60f, 12f), alpha = .7f, style = Stroke(2f))
    }
    drawOval(Color(0xFF4FAE5F), Offset(75f, 254f), Size(18f, 8f))
    drawCircle(Color(0xFFF7A1B8), 3f, Offset(84f, 256f))

    for ((x, y, c) in flowers) {
        drawLine(Color(0xFF3D8A30), Offset(x, y + 8f), Offset(x, y), 2f)
        drawCircle(Color(c), 4.5f, Offset(x, y))
        drawCircle(Color(0xFF7A5644), 4.5f, Offset(x, y), style = Stroke(1f))
        drawCircle(Color(0xFFF08A32), 1.6f, Offset(x, y))
    }
    for ((x, y) in tufts) {
        val tuft = Path().apply {
            moveTo(x, y)
            quadraticTo(x + 2, y - 10, x + 4, y)
            quadraticTo(x + 6, y - 12, x + 8, y)
            quadraticTo(x + 10, y - 9, x + 12, y)
        }
        drawPath(tuft, Color(0xFF4E9A3A), style = Stroke(2.5f, cap = StrokeCap.Round))
    }

    butterfly(t * 5f, Color(0xFFF7A1B8), 120f to 230f, 200f to 190f, 260f to 240f, 170f to 260f)
    butterfly(t * 4f, Color(0xFFFFD25A), 330f to 250f, 250f to 220f, 300f to 280f)
}

private fun DrawScope.cloud(x: Float, y: Float, alpha: Float) {
    translate(x, y) {
        drawOval(Color.White, Offset(36f, 37f), Size(68f, 26f), alpha = alpha)
        drawOval(Color.White, Offset(68f, 28f), Size(40f, 28f), alpha = alpha)
        drawOval(Color.White, Offset(42f, 34f), Size(28f, 20f), alpha = alpha)
    }
}

/** A butterfly looping through [points]; wings flap by squashing vertically. */
private fun DrawScope.butterfly(t: Float, color: Color, vararg points: Pair<Float, Float>) {
    val n = points.size
    val pos = (t % 1f) * n
    val i = pos.toInt() % n
    val f = pos - pos.toInt()
    val e = f * f * (3 - 2 * f)
    val (x0, y0) = points[i]
    val (x1, y1) = points[(i + 1) % n]
    val x = x0 + (x1 - x0) * e
    val y = y0 + (y1 - y0) * e
    val flap = .2f + .8f * kotlin.math.abs(sin(t * 400f))
    scale(1f, flap, pivot = Offset(x, y)) { drawOval(color, Offset(x - 6f, y - 4f), Size(12f, 8f)) }
    drawOval(Color(0xFF5A3D2E), Offset(x - 1.5f, y - 4f), Size(3f, 8f))
}

@Preview(widthDp = 360, heightDp = 280)
@Composable
private fun MeadowScenePreview() {
    MeadowScene(animate = false)
}
