package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme

private val TeacherGradient = Brush.linearGradient(listOf(StemColors.Teal, StemColors.TealBlue))

/** The teal-to-blue header of Mooshala's teacher screens, with optional actions on the right. */
@Composable
fun TeacherHeader(
    title: BilingualString,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(TeacherGradient)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            navigation?.invoke()
            BilingualText(
                text = title,
                size = BilingualTextSize.Large,
                regionalColor = Color.White,
                englishColor = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            actions()
        }
    }
}

@BilingualPreviews
@Composable
private fun TeacherHeaderPreview() {
    StemTutorTheme { TeacherHeader(BilingualString("नमस्ते, राव मैडम", "Hello, Mrs. Rao")) }
}
