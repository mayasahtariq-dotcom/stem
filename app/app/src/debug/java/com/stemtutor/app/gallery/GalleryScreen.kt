package com.stemtutor.app.gallery

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stemtutor.app.R
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.designsystem.component.AnswerOption
import com.stemtutor.core.designsystem.component.AnswerOptionState
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.EmptyScreen
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.SourceChip
import com.stemtutor.core.designsystem.component.TopStatsBar
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.designsystem.R as DsR

@Composable
fun GalleryRoute(viewModel: GalleryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GalleryScreen(
        state = state,
        onLanguageModeChange = viewModel::setLanguageMode,
        onOptionSelected = viewModel::selectOption,
        onRetryBackend = viewModel::refreshBackend,
    )
}

private val sampleOptions = listOf(
    DsR.string.ds_sample_option_nucleus,
    DsR.string.ds_sample_option_mitochondria,
    DsR.string.ds_sample_option_ribosome,
)

/** Stateless gallery of every design-system component. */
@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onLanguageModeChange: (LanguageMode) -> Unit,
    onOptionSelected: (Int) -> Unit,
    onRetryBackend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = StemColors.White,
        topBar = {
            TopStatsBar(
                streakDays = state.streakDays,
                xp = state.xp,
                hearts = state.hearts,
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("gallery"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BilingualText(
                    textRes = R.string.gallery_title,
                    size = BilingualTextSize.Large,
                    regionalColor = StemColors.DeepBlue,
                )
            }
            item { LanguageToggle(state.languageMode, onLanguageModeChange) }

            item { SectionTitle(R.string.gallery_section_text) }
            items(BilingualTextSize.entries.size) { i ->
                BilingualText(DsR.string.ds_sample_question, size = BilingualTextSize.entries[i])
            }

            item { SectionTitle(R.string.gallery_section_buttons) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val full = Modifier.fillMaxWidth()
                    ChunkyButton(DsR.string.ds_sample_check, {}, full, ChunkyButtonStyle.Primary)
                    ChunkyButton(DsR.string.ds_sample_continue, {}, full, ChunkyButtonStyle.Cream)
                    ChunkyButton(DsR.string.ds_sample_skip, {}, full, ChunkyButtonStyle.Neutral)
                    ChunkyButton(DsR.string.ds_sample_check, {}, full, enabled = false)
                }
            }

            item { SectionTitle(R.string.gallery_section_options) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sampleOptions.forEachIndexed { index, res ->
                        AnswerOption(
                            textRes = res,
                            state = if (state.selectedOption == index) {
                                AnswerOptionState.Selected
                            } else {
                                AnswerOptionState.Default
                            },
                            onClick = { onOptionSelected(index) },
                        )
                    }
                }
            }
            item { SectionTitle(R.string.gallery_section_option_states) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnswerOptionState.entries.forEach { optionState ->
                        AnswerOption(DsR.string.ds_sample_option_mitochondria, optionState, {})
                    }
                }
            }

            item { SectionTitle(R.string.gallery_section_stats) }
            item { TopStatsBar(streakDays = 12, xp = 340, hearts = 2) }

            item { SectionTitle(R.string.gallery_section_source) }
            item { SourceChip(page = 2) }

            item { SectionTitle(R.string.gallery_section_states) }
            item { Framed { LoadingScreen() } }
            item { Framed { ErrorScreen(onRetry = {}) } }
            item { Framed { EmptyScreen(onRetry = {}) } }

            item { SectionTitle(R.string.gallery_section_backend) }
            item { BackendStatus(state.backend, onRetryBackend) }
        }
    }
}

@Composable
private fun LanguageToggle(mode: LanguageMode, onChange: (LanguageMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            LanguageMode.HINDI to BilingualString("हिन्दी + English", "Hindi"),
            LanguageMode.KANNADA to BilingualString("ಕನ್ನಡ + English", "Kannada"),
        ).forEach { (option, label) ->
            ChunkyButton(
                label = label,
                onClick = { onChange(option) },
                modifier = Modifier.weight(1f).testTag("language_${option.localeTag}"),
                style = if (option == mode) ChunkyButtonStyle.Cream else ChunkyButtonStyle.Neutral,
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes res: Int) {
    BilingualText(
        textRes = res,
        size = BilingualTextSize.Large,
        regionalColor = StemColors.DeepBlue,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun Framed(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .border(1.dp, StemColors.NeutralBorder, RoundedCornerShape(12.dp)),
    ) { content() }
}

@Composable
private fun BackendStatus(backend: BackendState, onRetry: () -> Unit) {
    when (backend) {
        BackendState.Loading -> BilingualText(DsR.string.ds_loading)
        is BackendState.Connected -> BilingualText(
            text = bilingualStringResource(R.string.gallery_backend_ok, backend.version),
            regionalColor = StemColors.Green,
        )
        BackendState.Unreachable -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BilingualText(R.string.gallery_backend_error, regionalColor = StemColors.Red)
            ChunkyButton(DsR.string.ds_retry, onRetry, style = ChunkyButtonStyle.Neutral)
        }
    }
}

@BilingualPreviews
@Composable
private fun GalleryScreenPreview() {
    StemTutorTheme {
        GalleryScreen(
            state = GalleryUiState(selectedOption = 1, backend = BackendState.Connected("fake")),
            onLanguageModeChange = {},
            onOptionSelected = {},
            onRetryBackend = {},
        )
    }
}
