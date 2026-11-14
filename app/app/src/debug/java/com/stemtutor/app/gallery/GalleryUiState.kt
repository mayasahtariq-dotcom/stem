package com.stemtutor.app.gallery

import com.stemtutor.core.common.language.LanguageMode

sealed interface BackendState {
    data object Loading : BackendState
    data class Connected(val version: String) : BackendState
    data object Unreachable : BackendState
}

data class GalleryUiState(
    val languageMode: LanguageMode = LanguageMode.DEFAULT,
    val selectedOption: Int? = null,
    val backend: BackendState = BackendState.Loading,
    val streakDays: Int = 5,
    val xp: Int = 120,
    val hearts: Int = 4,
)
