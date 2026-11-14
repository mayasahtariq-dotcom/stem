package com.stemtutor.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.designsystem.R

// Only regular, bold and extra-bold are bundled, to keep the app small.
val NotoSans = FontFamily(
    Font(R.font.noto_sans_regular, FontWeight.Normal),
    Font(R.font.noto_sans_bold, FontWeight.Bold),
    Font(R.font.noto_sans_extrabold, FontWeight.ExtraBold),
)

val NotoSansDevanagari = FontFamily(
    Font(R.font.noto_sans_devanagari_regular, FontWeight.Normal),
    Font(R.font.noto_sans_devanagari_bold, FontWeight.Bold),
    Font(R.font.noto_sans_devanagari_extrabold, FontWeight.ExtraBold),
)

val NotoSansKannada = FontFamily(
    Font(R.font.noto_sans_kannada_regular, FontWeight.Normal),
    Font(R.font.noto_sans_kannada_bold, FontWeight.Bold),
    Font(R.font.noto_sans_kannada_extrabold, FontWeight.ExtraBold),
)

fun regionalFontFamily(mode: LanguageMode): FontFamily = when (mode) {
    LanguageMode.HINDI -> NotoSansDevanagari
    LanguageMode.KANNADA -> NotoSansKannada
}

/** Headings: Noto Sans extra bold, 20–24 sp, deep blue. */
val StemTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(fontFamily = NotoSans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = NotoSans, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
