package com.stemtutor.feature.student

import androidx.annotation.StringRes
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.designsystem.R as DsR

@StringRes
fun Subject.nameRes(): Int = when (this) {
    Subject.BIOLOGY -> DsR.string.subject_biology
    Subject.CHEMISTRY -> DsR.string.subject_chemistry
    Subject.PHYSICS -> DsR.string.subject_physics
}

@StringRes
fun LanguageMode.nameRes(): Int = when (this) {
    LanguageMode.HINDI -> DsR.string.language_hindi
    LanguageMode.KANNADA -> DsR.string.language_kannada
}
