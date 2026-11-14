package com.stemtutor.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Mooshala design tokens (Finch-style: white screens, green 3D buttons, blue celebration
 * screens). Replaces the PRD palette; see docs/decisions.md.
 */
object StemColors {
    val White = Color(0xFFFFFFFF)

    val Green = Color(0xFF55B752) // primary buttons, progress, correct answers
    val GreenDark = Color(0xFF3F9A3D) // 3D edge under green, correct-answer text
    val Blue = Color(0xFF1499E0) // celebration screens, current lesson, selection
    val BlueDark = Color(0xFF0D7FC0)
    val Red = Color(0xFFE2574C) // wrong answers, hearts, alerts
    val Gold = Color(0xFFFFC23A) // stars, focus rings, highlights

    val Ink = Color(0xFF2E3135) // body text
    val Muted = Color(0xFF6B7682) // English lines and secondary text; never lighter
    val Line = Color(0xFFE7EAEE) // borders, empty bars
    val Soft = Color(0xFFF2F4F6) // speech bubbles, icon wells
    val Cream = Color(0xFFFFF8EE) // term cards
    val Brown = Color(0xFF8A6650) // chips, earthy accents
    val Outline = Color(0xFF5A3D2E) // sticker and mascot outlines

    // Tints and edges (not text colours).
    val BlueTint = Color(0xFFEAF6FD)
    val GreenTint = Color(0xFFECF8EB)
    val RedTint = Color(0xFFFDEEED)
    val CorrectText = Color(0xFF2C7A2A)
    val WrongText = Color(0xFFB23A30)
    val WhiteEdge = Color(0xFFC9D6E0)
    val DisabledEdge = Color(0xFFD5DADE)
    val ChipFill = Color(0xFFF6EAD8)
    val CreamBorder = Color(0xFFF0E2CC)

    // Teacher mode (Mooshala's livelier teacher screens).
    val Teal = Color(0xFF2BB3A3)
    val TealBlue = Color(0xFF1F8FB8)
    val TealDark = Color(0xFF1D8579)
    val TeacherBackground = Color(0xFFFFF8EC)
    val FieldFill = Color(0xFFFAFBFC)
    val KeyCream = Color(0xFFF3DFB8)
}
