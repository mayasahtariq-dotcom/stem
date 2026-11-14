package com.stemtutor.app.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.health.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val healthRepository: HealthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GalleryUiState())
    val state: StateFlow<GalleryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            languageRepository.languageMode.collect { mode ->
                _state.update { it.copy(languageMode = mode) }
            }
        }
        refreshBackend()
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch { languageRepository.setLanguageMode(mode) }
    }

    fun selectOption(index: Int) {
        _state.update { it.copy(selectedOption = index) }
    }

    fun refreshBackend() {
        _state.update { it.copy(backend = BackendState.Loading) }
        viewModelScope.launch {
            val backend = healthRepository.check().fold(
                onSuccess = { BackendState.Connected(it.version) },
                onFailure = { BackendState.Unreachable },
            )
            _state.update { it.copy(backend = backend) }
        }
    }
}
