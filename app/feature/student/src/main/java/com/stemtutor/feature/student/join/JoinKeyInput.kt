package com.stemtutor.feature.student.join

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.stemtutor.core.data.classroom.JoinKeys
import com.stemtutor.core.designsystem.component.CodeBoxes

/**
 * Eight boxes for a class key: letters are capitalised, focus moves to the next box, and the
 * dash is drawn for the student. [value] is the compact key without the dash ("BI09K4X2").
 */
@Composable
fun JoinKeyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    accessibilityLabel: String = "",
) {
    CodeBoxes(
        value = value,
        onValueChange = onValueChange,
        length = JoinKeys.LENGTH,
        sanitize = JoinKeys::sanitize,
        keyboardType = KeyboardType.Ascii,
        capitalization = KeyboardCapitalization.Characters,
        dashAfter = 4,
        isError = isError,
        enabled = enabled,
        accessibilityLabel = accessibilityLabel,
        modifier = modifier,
    )
}
