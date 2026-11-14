package com.stemtutor.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.designsystem.R

@OptIn(ExperimentalTextApi::class)
private fun rubik(weight: FontWeight) = Font(
    resId = R.font.rubik_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Mooshala's display font: headings, buttons and every English line. One variable file. */
val Rubik = FontFamily(
    rubik(FontWeight.Normal),
    rubik(FontWeight.Medium),
    rubik(FontWeight.Bold),
    rubik(FontWeight.ExtraBold),
)

// Rubik has no Devanagari or Kannada glyphs, so regional lines keep Noto Sans.
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

val StemTypography = Typography(
    displayLarge = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp),
    headlineMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Rubik,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(fontFamily = Rubik, fontWeight = FontWeight.Bold, fontSize = 17.sp),
)
