package com.stemtutor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.theme.regionalFontFamily
import com.stemtutor.core.designsystem.theme.Rubik
import com.stemtutor.core.designsystem.theme.StemColors

data class Tab(val icon: Sticker, val label: BilingualString)

/** Mooshala's bottom tab bar: sticker icon, regional label and a small English label. */
@Composable
fun BottomTabs(
    tabs: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    background: Color = StemColors.Green,
    content: Color = Color.White,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(background)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEachIndexed { index, tab ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (index == selected) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                    .selectable(selected = index == selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StickerIcon(tab.icon, size = 24.dp)
                Text(tab.label.regional, color = content, fontFamily = regionalFontFamily(currentLanguageMode()), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                Text(tab.label.english, color = content.copy(alpha = 0.85f), fontFamily = Rubik, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}
