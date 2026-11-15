package com.stemtutor.feature.student.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.EmptyScreen
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.bilingual
import com.stemtutor.feature.student.lesson.matches
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val COURSE_ARG = "courseId"

data class GlossaryUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val terms: List<Term> = emptyList(),
    val query: String = "",
    val savedOnly: Boolean = false,
    val open: Term? = null,
) {
    /** Saved terms or all of them, filtered by the search in any of the three languages. */
    val shown: List<Term> get() = terms.filter { (!savedOnly || it.saved) && it.matches(query) }
}

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val learning: LearningRepository,
    private val listen: ListenController,
    savedState: SavedStateHandle,
) : ViewModel() {
    val courseId: String = checkNotNull(savedState[COURSE_ARG])
    private val _state = MutableStateFlow(GlossaryUiState())
    val state: StateFlow<GlossaryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            learning.terms(courseId).fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, terms = list) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
            )
        }
    }

    fun search(q: String) = _state.update { it.copy(query = q) }

    fun showSaved(saved: Boolean) = _state.update { it.copy(savedOnly = saved) }

    fun open(key: String?) = _state.update { s -> s.copy(open = key?.let { k -> s.terms.firstOrNull { it.key == k } }) }

    fun toggleSaved(term: Term) {
        val saved = !term.saved
        viewModelScope.launch {
            learning.setTermSaved(courseId, term.key, saved).onSuccess {
                _state.update { s ->
                    val updated = term.copy(saved = saved)
                    s.copy(terms = s.terms.map { if (it.key == term.key) updated else it }, open = if (s.open?.key == term.key) updated else s.open)
                }
            }
        }
    }

    fun listen(text: Tri, mode: LanguageMode, englishOnly: Boolean) {
        viewModelScope.launch { listen.speak(text, mode, englishOnly) }
    }
}

@Composable
fun GlossaryRoute(onBack: () -> Unit, onAskDoubt: (courseId: String, termKey: String) -> Unit, viewModel: GlossaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode = currentLanguageMode()
    GlossaryScreen(
        state,
        onBack = onBack,
        onRetry = viewModel::load,
        onSearch = viewModel::search,
        onSavedOnly = viewModel::showSaved,
        onOpen = viewModel::open,
    )
    state.open?.let { term ->
        TermSheet(
            term = term,
            related = term.related.mapNotNull { k -> state.terms.firstOrNull { it.key == k } },
            onDismiss = { viewModel.open(null) },
            onListen = { text, englishOnly -> viewModel.listen(text, mode, englishOnly) },
            onToggleSave = { viewModel.toggleSaved(term) },
            onAskDoubt = { viewModel.open(null); onAskDoubt(viewModel.courseId, term.key) },
            onRelated = { viewModel.open(it) },
        )
    }
}

/** Saved terms and every term in the course, searchable in Hindi, Kannada or English. */
@Composable
fun GlossaryScreen(
    state: GlossaryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSearch: (String) -> Unit,
    onSavedOnly: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    animate: Boolean = true,
) {
    val back = bilingualStringResource(R.string.ln_back)
    Column(Modifier.fillMaxSize().background(StemColors.White)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "${back.regional} · ${back.english}", tint = StemColors.Ink)
            }
            BilingualText(R.string.ln_terms, size = BilingualTextSize.Large)
        }
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            else -> Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceCard(!state.savedOnly, { onSavedOnly(false) }, Modifier.weight(1f)) {
                        BilingualText(R.string.ln_tab_all, size = BilingualTextSize.Small)
                    }
                    ChoiceCard(state.savedOnly, { onSavedOnly(true) }, Modifier.weight(1f)) {
                        BilingualText(R.string.ln_tab_saved, size = BilingualTextSize.Small)
                    }
                }
                StemTextField(
                    value = state.query,
                    onValueChange = onSearch,
                    placeholder = bilingualStringResource(R.string.ln_search),
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    centered = false,
                )
                val shown = state.shown
                if (shown.isEmpty()) {
                    EmptyScreen(
                        onRetry = onRetry,
                        messageRes = if (state.savedOnly && state.query.isBlank()) R.string.ln_no_saved else R.string.ln_no_match,
                        animate = animate,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shown, key = { it.key }) { TermRow(it) { onOpen(it.key) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermRow(term: Term, onClick: () -> Unit) {
    val mode = currentLanguageMode()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StemColors.Cream)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BilingualText(BilingualString(term.forms.regional(mode), term.forms.en), size = BilingualTextSize.Large)
            BilingualText(term.meaning.bilingual(mode), size = BilingualTextSize.Small)
        }
        if (term.saved) StickerIcon(Sticker.Star, size = 24.dp)
    }
}

@BilingualPreviews
@Composable
private fun GlossaryPreview() {
    StemTutorTheme {
        GlossaryScreen(GlossaryUiState(loading = false, terms = listOf(PREVIEW_TERM.copy(saved = true))), {}, {}, {}, {}, {}, animate = false)
    }
}
