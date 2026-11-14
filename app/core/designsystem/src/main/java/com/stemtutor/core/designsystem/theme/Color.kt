package com.stemtutor.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Design tokens from the PRD. Text on baby blue or cream is always dark, never white. */
object StemColors {
    val White = Color(0xFFFFFFFF)
    val BabyBlue = Color(0xFFA9CBF0) // main buttons, headers, finished lessons
    val Cream = Color(0xFFF3DFB8) // current lesson, class key, highlights
    val DeepBlue = Color(0xFF2C5A88) // headings, links and outlines only
    val Green = Color(0xFF2E9E5B) // correct answers only
    val Red = Color(0xFFE5484D) // wrong answers, hearts, alerts
    val Muted = Color(0xFF5E6B64) // muted text; never use anything lighter for text

    val Ink = Color(0xFF1C2630) // body text, and text on baby blue / cream

    // Derived shades for chunky edges and tinted backgrounds (not text colours).
    val BabyBlueEdge = Color(0xFF7FA9D6)
    val BabyBlueTint = Color(0xFFE9F2FC)
    val CreamEdge = Color(0xFFD8BD88)
    val GreenTint = Color(0xFFE3F4EA)
    val RedTint = Color(0xFFFDE8E9)
    val NeutralBorder = Color(0xFFD5DCE3)
    val DisabledFill = Color(0xFFECEFF2)
}
