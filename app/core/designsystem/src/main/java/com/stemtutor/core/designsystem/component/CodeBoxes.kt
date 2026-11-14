package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

/**
 * A row of single-character boxes over one hidden text field (OTP codes, class keys). Typing
 * fills the next box; [sanitize] cleans and limits input (e.g. uppercases, drops invalid
 * characters). A dash is drawn after [dashAfter] boxes when set.
 */
@Composable
fun CodeBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    modifier: Modifier = Modifier,
    sanitize: (String) -> String = { it.take(length) },
    keyboardType: KeyboardType = KeyboardType.Number,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    dashAfter: Int? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    accessibilityLabel: String = "",
    onDone: () -> Unit = {},
) {
    BasicTextField(
        value = TextFieldValue(value, selection = TextRange(value.length)),
        onValueChange = { onValueChange(sanitize(it.text).take(length)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        modifier = modifier.semantics { if (accessibilityLabel.isNotEmpty()) contentDescription = accessibilityLabel },
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (i in 0 until length) {
                    if (dashAfter != null && i == dashAfter) {
                        Text("–", fontFamily = Rubik, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = StemColors.Muted)
                    }
                    CharBox(
                        char = value.getOrNull(i),
                        focused = enabled && i == value.length.coerceAtMost(length - 1),
                        isError = isError,
                    )
                }
            }
        },
    )
}

@Composable
private fun CharBox(char: Char?, focused: Boolean, isError: Boolean) {
    val border: Color = when {
        isError -> StemColors.Red
        focused -> StemColors.Blue
        char != null -> StemColors.Green
        else -> StemColors.Line
    }
    Box(
        Modifier
            .width(36.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isError) StemColors.RedTint else StemColors.FieldFill)
            .border(2.dp, border, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char?.toString().orEmpty(),
            fontFamily = Rubik,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = StemColors.Ink,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@BilingualPreviews
@Composable
private fun CodeBoxesPreview() {
    StemTutorTheme {
        CodeBoxes(value = "BI09K", onValueChange = {}, length = 8, dashAfter = 4, modifier = Modifier.padding(16.dp))
    }
}
