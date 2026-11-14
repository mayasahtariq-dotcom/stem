package com.stemtutor.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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

enum class ChunkyButtonStyle { Primary, Cream, Neutral }

private data class ChunkyColors(val face: Color, val edge: Color, val border: Color?, val text: Color)

private fun colorsFor(style: ChunkyButtonStyle, enabled: Boolean): ChunkyColors = when {
    !enabled -> ChunkyColors(StemColors.DisabledFill, StemColors.NeutralBorder, null, StemColors.Muted)
    style == ChunkyButtonStyle.Primary ->
        ChunkyColors(StemColors.BabyBlue, StemColors.BabyBlueEdge, null, StemColors.Ink)
    style == ChunkyButtonStyle.Cream ->
        ChunkyColors(StemColors.Cream, StemColors.CreamEdge, null, StemColors.Ink)
    else -> ChunkyColors(
        StemColors.White,
        StemColors.NeutralBorder,
        StemColors.NeutralBorder,
        StemColors.Ink,
    )
}

private val EdgeDepth = 4.dp
private val ButtonShape = RoundedCornerShape(16.dp)

/**
 * Duolingo-style button: a thick bottom edge that disappears (the face drops) while pressed.
 * Disabled is `enabled = false`, in any style.
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
    val sunk = pressed && enabled

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        // The edge (bottom lip), always EdgeDepth below the resting face.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = EdgeDepth)
                .clip(ButtonShape)
                .background(colors.edge),
        )
        // The face: drops by EdgeDepth while pressed, covering the edge.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (sunk) EdgeDepth else 0.dp, bottom = if (sunk) 0.dp else EdgeDepth)
                .clip(ButtonShape)
                .background(colors.face)
                .then(colors.border?.let { Modifier.border(2.dp, it, ButtonShape) } ?: Modifier)
                .heightIn(min = 48.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BilingualText(
                text = label,
                size = BilingualTextSize.Small,
                regionalColor = colors.text,
                englishColor = if (enabled) StemColors.Ink.copy(alpha = 0.8f) else StemColors.Muted,
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
            ChunkyButton(R.string.ds_sample_continue, {}, mod, ChunkyButtonStyle.Cream)
            ChunkyButton(R.string.ds_sample_skip, {}, mod, ChunkyButtonStyle.Neutral)
            ChunkyButton(R.string.ds_sample_check, {}, mod, enabled = false)
        }
    }
}
