package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/**
 * Mooshala button styles: [Primary] green with white text (one per screen), [White] for
 * secondary actions and on blue/coloured screens, [Ghost] a flat text button.
 */
enum class ChunkyButtonStyle { Primary, White, Ghost }

private data class ChunkyColors(val face: Color, val edge: Color, val text: Color, val english: Color)

private fun colorsFor(style: ChunkyButtonStyle, enabled: Boolean): ChunkyColors = when {
    style == ChunkyButtonStyle.Ghost ->
        ChunkyColors(Color.Transparent, Color.Transparent, StemColors.Muted, StemColors.Muted)
    !enabled -> ChunkyColors(StemColors.Line, StemColors.DisabledEdge, StemColors.Muted, StemColors.Muted)
    style == ChunkyButtonStyle.Primary ->
        ChunkyColors(StemColors.Green, StemColors.GreenDark, StemColors.White, StemColors.White.copy(alpha = 0.85f))
    else -> ChunkyColors(StemColors.White, StemColors.WhiteEdge, StemColors.Ink, StemColors.Muted)
}

private val EdgeDepth = 5.dp
private val ButtonShape = RoundedCornerShape(14.dp)

/**
 * Finch/Duolingo-style 3D button: a solid bottom edge; the face drops onto it while pressed.
 * Disabled is `enabled = false`.
 */
@Composable
fun ChunkyButton(
    label: BilingualString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChunkyButtonStyle = ChunkyButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = colorsFor(style, enabled)
    val flat = style == ChunkyButtonStyle.Ghost
    val depth = if (flat) 0.dp else EdgeDepth
    val drop by animateDpAsState(
        targetValue = if (pressed && enabled && !flat) 4.dp else 0.dp,
        animationSpec = tween(80),
        label = "press",
    )

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        if (!flat) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(top = depth)
                    .clip(ButtonShape)
                    .background(colors.edge),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = depth)
                .offset(y = drop)
                .clip(ButtonShape)
                .background(colors.face)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = if (flat) 6.dp else 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            BilingualText(
                text = label,
                size = BilingualTextSize.Large,
                regionalColor = colors.text,
                englishColor = colors.english,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ChunkyButton(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChunkyButtonStyle = ChunkyButtonStyle.Primary,
    enabled: Boolean = true,
) {
    ChunkyButton(bilingualStringResource(labelRes), onClick, modifier, style, enabled)
}

@BilingualPreviews
@Composable
private fun ChunkyButtonPreview() {
    StemTutorTheme {
        Column(Modifier.padding(16.dp)) {
            val mod = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ChunkyButton(R.string.ds_sample_check, {}, mod, ChunkyButtonStyle.Primary)
            ChunkyButton(R.string.ds_sample_continue, {}, mod, ChunkyButtonStyle.White)
            ChunkyButton(R.string.ds_sample_skip, {}, mod, ChunkyButtonStyle.Ghost)
            ChunkyButton(R.string.ds_sample_check, {}, mod, enabled = false)
        }
    }
}
