package com.stemtutor.core.designsystem.mascot

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.theme.Rubik
import kotlin.math.abs

/**
 * The Mooshala mascot: a chibi cream cow with brown patches, drawn in code and gently animated
 * (bobbing, blinking, ear twitch, tail swish, plus one motion per pose). Ported from the
 * prototype's SVG, viewBox (-14, -14, 228, 248).
 */
enum class CowPose { Wave, Heart, Thumbs, Question, Sleep, Cool, Sad }

private const val VIEW_W = 228f
private const val VIEW_H = 248f
private const val ORIGIN = 14f

/** Animation phases, each 0..1 and looping. All zero when animation is off. */
private class CowClock(
    val bob: Float = 0f,
    val blink: Float = 0f,
    val ear: Float = 0f,
    val tail: Float = 0f,
    val fast: Float = 0f,
    val pose: Float = 0f,
    val beat: Float = 0f,
    val float: Float = 0f,
)

@Composable
private fun loop(transition: InfiniteTransition, ms: Int): State<Float> =
    transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart),
        label = "cow$ms",
    )

/**
 * @param animate turn off for static uses (lists, tests); the cow is then drawn at rest.
 * @param contentDescription null keeps the mascot decorative.
 */
@Composable
fun CowMascot(
    pose: CowPose,
    modifier: Modifier = Modifier,
    width: Dp = 118.dp,
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val measurer = rememberTextMeasurer()
    val clock: () -> CowClock
    if (animate) {
        val t = rememberInfiniteTransition(label = "cow")
        val bob = loop(t, 2400)
        val blink = loop(t, 4200)
        val ear = loop(t, 3400)
        val tail = loop(t, 2600)
        val fast = loop(t, poseFastMs(pose))
        val poseLoop = loop(t, poseMs(pose))
        val beat = loop(t, 900)
        val float = loop(t, 2400)
        clock = {
            CowClock(bob.value, blink.value, ear.value, tail.value, fast.value, poseLoop.value, beat.value, float.value)
        }
    } else {
        clock = { CowClock() }
    }
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.size(width, width * (VIEW_H / VIEW_W)).then(semantics)) {
        val c = clock()
        val k = size.width / VIEW_W
        withTransform({
            scale(k, k, pivot = Offset.Zero)
            translate(ORIGIN, ORIGIN + tri(c.bob) * -6f)
        }) {
            drawCow(pose, c, measurer)
        }
    }
}

private fun poseMs(pose: CowPose) = when (pose) {
    CowPose.Wave -> 1800
    CowPose.Heart -> 1400
    CowPose.Thumbs -> 1200
    CowPose.Question -> 2400
    CowPose.Sleep -> 2800
    CowPose.Cool -> 2000
    CowPose.Sad -> 2600
}

private fun poseFastMs(pose: CowPose) = when (pose) {
    CowPose.Wave -> 900
    CowPose.Thumbs -> 1000
    CowPose.Question -> 2000
    CowPose.Sad -> 1200
    else -> 1100
}

// ---- easing helpers -------------------------------------------------------------------------

/** 0 → 1 → 0 over one loop, eased (CSS `alternate`). */
private fun tri(p: Float): Float {
    val x = 1f - abs(2f * p - 1f)
    return x * x * (3f - 2f * x)
}

/** Piecewise-linear keyframes over p in 0..1: pairs of (time, value). */
private fun keys(p: Float, vararg kv: Float): Float {
    var i = 0
    while (i + 2 < kv.size && p > kv[i + 2]) i += 2
    val (t0, v0) = kv[i] to kv[i + 1]
    if (i + 2 >= kv.size) return v0
    val (t1, v1) = kv[i + 2] to kv[i + 3]
    val f = if (t1 == t0) 1f else ((p - t0) / (t1 - t0)).coerceIn(0f, 1f)
    return v0 + (v1 - v0) * (f * f * (3f - 2f * f))
}

// ---- art ----------------------------------------------------------------------------------

private val O = Color(0xFF5B3A29) // outline
private val W = Color(0xFFFFFAF2) // cream
private val SP = Color(0xFF8A5A44) // spots
private val PK = Color(0xFFF6B3B6) // inner ear
private val HF = Color(0xFF6E4733) // hooves
private val EyeDark = Color(0xFF2A1B14)
private val Pink = Color(0xFFF7889A)
private val Spark = Color(0xFFFFC23A)
private val SkyBlue = Color(0xFF3B8DE0)
private val Tear = Color(0xFF5AAEE8)

private val Outline = Stroke(width = 3.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()

private object Art {
    val head by lazy { path("M100 24C150 24 176 46 176 84C176 122 148 142 100 142C52 142 24 122 24 84C24 46 50 24 100 24Z") }
    val body by lazy { path("M62 128C52 160 54 196 74 204C90 209 110 209 126 204C146 196 148 160 138 128Z") }
    val tailLine by lazy { path("M136 182q26 0 28-24") }
    val tailTuft by lazy { path("M164 160q-12-8-6-22 14 6 13 20z") }
    val bodySpot1 by lazy { path("M134 138C150 148 152 172 140 182C126 178 122 156 134 138Z") }
    val bodySpot2 by lazy { path("M60 170C72 164 82 174 80 188C70 196 58 188 60 170Z") }
    val legL by lazy { path("M74 182v20a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-20") }
    val legR by lazy { path("M106 182v20a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-20") }
    val hoofL by lazy { path("M74 198v4a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-4z") }
    val hoofR by lazy { path("M106 198v4a8 8 0 0 0 8 8h4a8 8 0 0 0 8-8v-4z") }
    val hornL by lazy { path("M64 36q-8-20 4-30 9 10 12 24z") }
    val hornR by lazy { path("M136 36q8-20-4-30-9 10-12 24z") }
    val earL by lazy { path("M34 70C18 62 4 70 2 82c10 8 26 8 36 0z") }
    val earLIn by lazy { path("M30 74C20 70 12 74 10 80c7 4 16 4 22 0z") }
    val earR by lazy { path("M166 70C182 62 196 70 198 82c-10 8-26 8-36 0z") }
    val earRIn by lazy { path("M170 74C180 70 188 74 190 80c-7 4-16 4-22 0z") }
    val headSpot1 by lazy { path("M116 22C150 18 176 40 176 68C154 74 136 64 126 48C120 38 114 30 116 22Z") }
    val headSpot2 by lazy { path("M24 92C36 84 52 90 52 104C50 116 36 120 24 112Z") }
    val tuft by lazy { path("M88 32q6-10 12-4 6-8 12 2") }
    val smile by lazy { path("M91 120q9 9 18 0") }
    val frown by lazy { path("M93 125q7-6 14 0") }
    val shutL by lazy { path("M65 90q9 6 18 0") }
    val shutR by lazy { path("M117 90q9 6 18 0") }
    val wink by lazy { path("M134 82l-13 7 13 7") }
    val shadesBar by lazy { path("M46 84h108") }
    val shades by lazy { path("M50 82h44q0 24-22 24t-22-24zM106 82h44q0 24-22 24t-22-24z") }
    val shadesGlint by lazy { path("M58 88h10") }
    val sadBrows by lazy { path("M62 70l16 5M138 70l-16 5") }
    val waveLines by lazy { path("M178 84l10-6M182 100h12M178 116l10 6") }
    val bigHeart by lazy { path("M100 196c-24-14-32-23-32-33 0-10 11-15 20-8l12 9 12-9c9-7 20-2 20 8 0 10-8 19-32 33z") }
    val heartGlint by lazy { path("M82 162q2-6 8-6") }
    val floatHeart by lazy { path("M182 46c-9-6-12-9-12-13 0-4 4-6 7-3l5 4 5-4c3-3 7-1 7 3 0 4-3 7-12 13z") }
    val star by lazy { path("M0-11l3 8 8 3-8 3-3 8-3-8-8-3 8-3z") }
    val tear by lazy { path("M146 96q7 11 0 15q-7-4 0-15z") }
}

private val headBrush = Brush.radialGradient(
    0f to Color.White, 0.7f to W, 1f to Color(0xFFF1E4D0),
    center = Offset(82f, 59f),
    radius = 122f,
)
private val bodyBrush = Brush.radialGradient(
    0f to Color.White, 0.7f to W, 1f to Color(0xFFF1E4D0),
    center = Offset(88f, 152f),
    radius = 78f,
)
private val muzzleBrush = Brush.verticalGradient(
    0f to Color(0xFFFCD2C2), 1f to Color(0xFFF3AB98),
    startY = 92f,
    endY = 136f,
)

private fun DrawScope.fillAndOutline(p: Path, brush: Brush) {
    drawPath(p, brush)
    drawPath(p, O, style = Outline)
}

private fun DrawScope.fillAndOutline(p: Path, color: Color) {
    drawPath(p, color)
    drawPath(p, O, style = Outline)
}

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float = 1f) =
    drawOval(color, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = alpha)

private fun DrawScope.arm(x: Float, y: Float, deg: Float, fill: Color = W) {
    rotate(deg, pivot = Offset(x, y)) {
        drawOval(fill, Offset(x - 11, y - 16), Size(22f, 32f))
        drawOval(O, Offset(x - 11, y - 16), Size(22f, 32f), style = Outline)
    }
}

private fun DrawScope.roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
    val radius = androidx.compose.ui.geometry.CornerRadius(r)
    drawRoundRect(W, Offset(x, y), Size(w, h), radius)
    drawRoundRect(O, Offset(x, y), Size(w, h), radius, style = Outline)
}

private fun DrawScope.sparkle(x: Float, y: Float, s: Float, alpha: Float) {
    translate(x, y) { scale(s, s, pivot = Offset.Zero) { drawPath(Art.star, Spark, alpha = alpha) } }
}

private fun DrawScope.eye(x: Float) {
    oval(x, 88f, 9f, 11.5f, EyeDark)
    oval(x, 93f, 6f, 5f, Color(0xFF4A3024))
    drawCircle(Color.White, 3.8f, Offset(x + 3, 83f))
    drawCircle(Color.White, 1.7f, Offset(x - 3.2f, 93.5f))
}

private val EyeLine = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawCow(pose: CowPose, c: CowClock, measurer: TextMeasurer) {
    val pivot = Offset(100f, 226f)
    withTransform({ poseMotion(pose, c.pose, pivot) }) {
        oval(100f, 214f, 56f, 7f, Color(0x1F3C281E))

        rotate(tri(c.tail) * 14f, pivot = Offset(136f, 182f)) {
            drawPath(Art.tailLine, O, style = Outline)
            fillAndOutline(Art.tailTuft, SP)
        }

        fillAndOutline(Art.body, bodyBrush)
        clipPath(Art.body) {
            drawPath(Art.bodySpot1, SP)
            drawPath(Art.bodySpot2, SP)
            oval(100f, 132f, 44f, 9f, Color(0x215B3A29))
        }
        fillAndOutline(Art.legL, W)
        fillAndOutline(Art.legR, W)
        drawPath(Art.hoofL, HF)
        drawPath(Art.hoofR, HF)

        drawHead(pose, c)
        drawPoseItems(pose, c, measurer)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawTransform.poseMotion(pose: CowPose, p: Float, pivot: Offset) {
    when (pose) {
        CowPose.Wave -> rotate(keys(p, 0f, 0f, .25f, -4f, .75f, 4f, 1f, 0f), pivot)
        CowPose.Heart -> {
            val sx = keys(p, 0f, 1f, .3f, 1.05f, .55f, .98f, 1f, 1f)
            val sy = keys(p, 0f, 1f, .3f, .96f, .55f, 1.03f, 1f, 1f)
            scale(sx, sy, pivot)
        }
        CowPose.Thumbs -> translate(top = keys(p, 0f, 0f, .4f, -10f, .6f, 0f, 1f, 0f))
        CowPose.Question -> rotate(keys(p, 0f, 0f, .5f, -6f, 1f, 0f), pivot)
        CowPose.Sleep -> scale(1f + tri(p) * .03f, 1f - tri(p) * .03f, pivot)
        CowPose.Cool -> rotate(-3f + tri(p) * 6f, pivot)
        CowPose.Sad -> translate(left = keys(p, 0f, 0f, .9f, 0f, .93f, -3f, .96f, 3f, 1f, 0f))
    }
}

private fun DrawScope.drawHead(pose: CowPose, c: CowClock) {
    fillAndOutline(Art.hornL, Color(0xFFF1CF8F))
    fillAndOutline(Art.hornR, Color(0xFFF1CF8F))

    rotate(keys(c.ear, 0f, 0f, .8f, 0f, .9f, -12f, 1f, 0f), pivot = Offset(36f, 76f)) {
        fillAndOutline(Art.earL, W)
        drawPath(Art.earLIn, PK)
    }
    rotate(keys(c.ear, 0f, 0f, .1f, 12f, .2f, 0f, 1f, 0f), pivot = Offset(164f, 76f)) {
        fillAndOutline(Art.earR, W)
        drawPath(Art.earRIn, PK)
    }

    fillAndOutline(Art.head, headBrush)
    clipPath(Art.head) {
        drawPath(Art.headSpot1, SP)
        drawPath(Art.headSpot2, SP)
        drawCircle(SP, 6f, Offset(72f, 48f), alpha = .85f)
    }
    drawPath(Art.tuft, O, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    val blink = if (pose == CowPose.Cool || pose == CowPose.Sleep) 1f else keys(c.blink, 0f, 1f, .94f, 1f, .97f, .1f, 1f, 1f)
    scale(1f, blink, pivot = Offset(100f, 88f)) { drawEyes(pose) }

    oval(54f, 110f, 12f, 7f, Color(0xFFF59A92), alpha = .5f)
    oval(146f, 110f, 12f, 7f, Color(0xFFF59A92), alpha = .5f)
    drawOval(muzzleBrush, Offset(66f, 92f), Size(68f, 44f))
    oval(94f, 104f, 12f, 4f, Color.White, alpha = .35f)
    oval(88f, 110f, 4f, 5.5f, Color(0xFFB9685A))
    oval(112f, 110f, 4f, 5.5f, Color(0xFFB9685A))

    val mouthStroke = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (pose == CowPose.Sad) {
        drawPath(Art.frown, Color(0xFF7A3E30), style = mouthStroke)
    } else {
        drawPath(Art.smile, Color(0xFFE9776C))
        drawPath(Art.smile, Color(0xFF7A3E30), style = mouthStroke)
    }
}

private fun DrawScope.drawEyes(pose: CowPose) {
    when (pose) {
        CowPose.Sleep -> {
            drawPath(Art.shutL, EyeDark, style = EyeLine)
            drawPath(Art.shutR, EyeDark, style = EyeLine)
        }
        CowPose.Thumbs -> {
            eye(74f)
            drawPath(Art.wink, EyeDark, style = EyeLine)
        }
        CowPose.Cool -> {
            drawPath(Art.shadesBar, Color(0xFF1D1D1D), style = Stroke(4f))
            drawPath(Art.shades, Color(0xFF1D1D1D))
            drawPath(Art.shadesGlint, Color.White, alpha = .55f, style = Stroke(3f, cap = StrokeCap.Round))
        }
        CowPose.Sad -> {
            eye(74f)
            eye(126f)
            drawPath(Art.sadBrows, EyeDark, style = Stroke(3.4f, cap = StrokeCap.Round))
        }
        else -> {
            eye(74f)
            eye(126f)
        }
    }
}

private fun DrawScope.drawPoseItems(pose: CowPose, c: CowClock, measurer: TextMeasurer) {
    val twinkle = 1f - tri(c.fast) * .75f
    when (pose) {
        CowPose.Wave -> {
            arm(56f, 152f, 25f)
            rotate(-16f + tri(c.fast) * 34f, pivot = Offset(146f, 130f)) { arm(160f, 112f, -150f) }
            drawPath(Art.waveLines, O, alpha = twinkle, style = Stroke(3.4f, cap = StrokeCap.Round))
        }
        CowPose.Heart -> {
            val s = keys(c.beat, 0f, 1f, .3f, 1.1f, 1f, 1f)
            scale(s, s, pivot = Offset(100f, 178f)) {
                fillAndOutline(Art.bigHeart, Pink)
                drawPath(Art.heartGlint, Color.White, alpha = .7f, style = Stroke(3f, cap = StrokeCap.Round))
            }
            arm(72f, 166f, 40f)
            arm(128f, 166f, -40f)
            floating(c.float) { drawPath(Art.floatHeart, Pink, alpha = it) }
        }
        CowPose.Thumbs -> {
            arm(56f, 152f, 25f)
            val lift = tri(c.fast)
            translate(top = lift * -8f) {
                rotate(lift * -8f, pivot = Offset(150f, 162f)) {
                    roundRect(138f, 140f, 22f, 22f, 9f)
                    roundRect(143f, 121f, 10f, 24f, 5f)
                }
            }
            sparkle(178f, 86f, 1f, twinkle)
            sparkle(190f, 112f, .6f, twinkle)
        }
        CowPose.Question -> {
            arm(56f, 152f, 25f)
            rotate(tri(c.fast) * 4f, pivot = Offset(128f, 154f)) { arm(128f, 140f, -10f) }
            val hop = tri(c.fast) * -6f
            text(measurer, "?", 164f, 40f + hop, 42f)
        }
        CowPose.Sleep -> {
            arm(56f, 152f, 25f)
            arm(144f, 152f, -25f)
            listOf(Triple("Z", Offset(150f, 36f), 30f), Triple("z", Offset(172f, 14f), 22f), Triple("z", Offset(188f, -4f), 15f))
                .forEachIndexed { i, (s, at, size) ->
                    val p = (c.float + i * .25f) % 1f
                    val alpha = keys(p, 0f, 0f, .3f, 1f, 1f, 0f)
                    text(measurer, s, at.x, at.y + 10f - p * 40f, size, alpha)
                }
        }
        CowPose.Cool -> {
            roundRect(60f, 146f, 80f, 24f, 12f)
            sparkle(184f, 54f, 1.2f, twinkle)
        }
        CowPose.Sad -> {
            arm(56f, 152f, 25f)
            arm(144f, 152f, -25f)
            val p = c.fast
            translate(top = -8f + p * 34f) {
                val a = keys(p, 0f, 0f, .3f, 1f, 1f, 0f)
                drawPath(Art.tear, Tear, alpha = a)
                drawPath(Art.tear, Color(0xFF3D7FB8), alpha = a, style = Stroke(1.5f))
            }
        }
    }
}

private inline fun DrawScope.floating(p: Float, block: DrawScope.(alpha: Float) -> Unit) {
    val alpha = keys(p, 0f, 0f, .3f, 1f, 1f, 0f)
    translate(top = 10f - p * 40f) { block(alpha) }
}

private fun DrawScope.text(measurer: TextMeasurer, s: String, x: Float, baseline: Float, sizePx: Float, alpha: Float = 1f) {
    // Font sizes are in viewBox units; the canvas is already scaled, so cancel density scaling.
    val style = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (sizePx / fontScale / density).sp,
        color = SkyBlue.copy(alpha = alpha),
    )
    val layout = measurer.measure(s, style)
    drawText(layout, topLeft = Offset(x, baseline - layout.firstBaseline))
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 420)
@Composable
private fun CowMascotPreview() {
    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CowPose.entries.forEach { CowMascot(it, width = 56.dp, animate = false) }
    }
}
