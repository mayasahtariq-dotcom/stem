package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.theme.StemColors

/** Mooshala's big rounded input (`.field`), with a "regional · English" placeholder. */
@Composable
fun StemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: BilingualString,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    centered: Boolean = true,
    maxLength: Int = 60,
    onDone: () -> Unit = {},
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    val style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = StemColors.Ink, textAlign = align)
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(StemColors.Blue),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${placeholder.regional}. ${placeholder.english}" },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(StemColors.FieldFill)
                    .border(2.dp, StemColors.Line, RoundedCornerShape(22.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        "${placeholder.regional} · ${placeholder.english}",
                        style = style.copy(color = StemColors.Muted, fontWeight = FontWeight.Medium, fontSize = 18.sp),
                    )
                }
                inner()
            }
        },
    )
}
