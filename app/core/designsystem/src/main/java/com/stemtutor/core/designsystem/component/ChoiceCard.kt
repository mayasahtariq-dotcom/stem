package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/** Mooshala's card look: 2 dp border with a thicker 4 dp bottom edge. */
fun Modifier.chunkyCard(fill: Color, border: Color, radius: Dp = 16.dp): Modifier =
    clip(RoundedCornerShape(radius))
        .background(fill)
        .drawBehind {
            val side = 2.dp.toPx()
            val bottom = 4.dp.toPx()
            val r = CornerRadius(radius.toPx())
            drawRoundRect(border, cornerRadius = r)
            drawRoundRect(
                fill,
                topLeft = Offset(side, side),
                size = Size(size.width - 2 * side, size.height - side - bottom),
                cornerRadius = CornerRadius(r.x - side),
            )
            drawRoundRect(border, cornerRadius = r, style = Stroke(side))
        }

/** A selectable card (language, role, subject choices). Blue when selected. */
@Composable
fun ChoiceCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .chunkyCard(
                fill = if (selected) StemColors.BlueTint else StemColors.White,
                border = if (selected) StemColors.Blue else StemColors.Line,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * Mooshala's big choice card (`.opt.big`): a large title, such as "हिंदी", with a bilingual
 * subtitle and an optional sticker icon.
 */
@Composable
fun BigChoiceCard(
    title: String,
    subtitle: BilingualString,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Sticker? = null,
) {
    ChoiceCard(selected, onClick, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) StickerIcon(icon, size = 30.dp)
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
        }
        BilingualText(subtitle, size = BilingualTextSize.Small, textAlign = TextAlign.Center)
    }
}

@BilingualPreviews
@Composable
private fun BigChoiceCardPreview() {
    StemTutorTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigChoiceCard("हिंदी", BilingualString("हिंदी पहले, नीचे English", "Hindi + English"), true, {})
            BigChoiceCard("ಕನ್ನಡ", BilingualString("ಕನ್ನಡ ಮೊದಲು, ಕೆಳಗೆ English", "Kannada + English"), false, {})
        }
    }
}
