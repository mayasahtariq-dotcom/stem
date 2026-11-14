package com.stemtutor.app.gallery

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.stemtutor.core.designsystem.component.ProgressBar
import com.stemtutor.core.designsystem.component.SourceChip
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.component.TopStatsBar
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.mascot.MeadowScene
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.Rubik
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

/** Stateless gallery of every design-system component, in the Mooshala style. */
@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onLanguageModeChange: (LanguageMode) -> Unit,
    onOptionSelected: (Int) -> Unit,
    onRetryBackend: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("gallery"),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MeadowScene(animate = animate) {
                TopStatsBar(
                    streakDays = state.streakDays,
                    xp = state.xp,
                    hearts = state.hearts,
                    modifier = Modifier.statusBarsPadding().align(Alignment.TopCenter),
                )
                CowMascot(
                    pose = CowPose.Wave,
                    animate = animate,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                BilingualText(
                    textRes = R.string.gallery_title,
                    size = BilingualTextSize.Large,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item { Padded { LanguageToggle(state.languageMode, onLanguageModeChange) } }

        section(R.string.gallery_section_text) {
            BilingualTextSize.entries.forEach {
                BilingualText(DsR.string.ds_sample_question, size = it)
            }
        }
        section(R.string.gallery_section_bubble) {
            SpeechBubble(DsR.string.ds_sample_question, Modifier.fillMaxWidth())
            CowMascot(CowPose.Question, animate = animate)
            ProgressBar(progress = 0.6f)
        }
        section(R.string.gallery_section_buttons) {
            val full = Modifier.fillMaxWidth()
            ChunkyButton(DsR.string.ds_sample_check, {}, full, ChunkyButtonStyle.Primary)
            ChunkyButton(DsR.string.ds_sample_continue, {}, full, ChunkyButtonStyle.White)
            ChunkyButton(DsR.string.ds_sample_skip, {}, full, ChunkyButtonStyle.Ghost)
            ChunkyButton(DsR.string.ds_sample_check, {}, full, enabled = false)
        }
        section(R.string.gallery_section_options) {
            sampleOptions.forEachIndexed { index, res ->
                AnswerOption(
                    textRes = res,
                    state = if (state.selectedOption == index) AnswerOptionState.Selected else AnswerOptionState.Default,
                    onClick = { onOptionSelected(index) },
                )
            }
        }
        section(R.string.gallery_section_option_states) {
            AnswerOptionState.entries.forEach { optionState ->
                AnswerOption(DsR.string.ds_sample_option_mitochondria, optionState, {})
            }
        }
        section(R.string.gallery_section_stats) {
            TopStatsBar(streakDays = 12, xp = 340, hearts = 2)
            SourceChip(page = 2)
        }
        section(R.string.gallery_section_mascot) { CowPoses(animate) }
        section(R.string.gallery_section_icons) { StickerGrid() }
        section(R.string.gallery_section_states) {
            Framed { LoadingScreen(animate = animate) }
            Framed { ErrorScreen(onRetry = {}, animate = animate) }
            Framed { EmptyScreen(onRetry = {}, animate = animate) }
        }
        section(R.string.gallery_section_backend) { BackendStatus(state.backend, onRetryBackend) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    @StringRes title: Int,
    content: @Composable () -> Unit,
) {
    item {
        Padded {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BilingualText(textRes = title, size = BilingualTextSize.Large, modifier = Modifier.padding(top = 16.dp))
                content()
            }
        }
    }
}

@Composable
private fun Padded(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 20.dp)) { content() }
}

@Composable
private fun LanguageToggle(mode: LanguageMode, onChange: (LanguageMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            LanguageMode.HINDI to BilingualString("हिंदी", "Hindi + English"),
            LanguageMode.KANNADA to BilingualString("ಕನ್ನಡ", "Kannada + English"),
        ).forEach { (option, label) ->
            AnswerOption(
                text = label,
                state = if (option == mode) AnswerOptionState.Selected else AnswerOptionState.Default,
                onClick = { onChange(option) },
                modifier = Modifier.weight(1f).testTag("language_${option.localeTag}"),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CowPoses(animate: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CowPose.entries.forEach { pose ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CowMascot(pose, width = 92.dp, animate = animate)
                Text(pose.name, fontFamily = Rubik, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StemColors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickerGrid() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Sticker.entries.forEach { StickerIcon(it, size = 32.dp) }
    }
}

@Composable
private fun Framed(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(420.dp)
            .border(2.dp, StemColors.Line, RoundedCornerShape(20.dp)),
    ) { content() }
}

@Composable
private fun BackendStatus(backend: BackendState, onRetry: () -> Unit) {
    when (backend) {
        BackendState.Loading -> BilingualText(DsR.string.ds_loading)
        is BackendState.Connected -> BilingualText(
            text = bilingualStringResource(R.string.gallery_backend_ok, backend.version),
            regionalColor = StemColors.CorrectText,
        )
        BackendState.Unreachable -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BilingualText(R.string.gallery_backend_error, regionalColor = StemColors.WrongText)
            ChunkyButton(DsR.string.ds_retry, onRetry, style = ChunkyButtonStyle.White)
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
            animate = false,
        )
    }
}
