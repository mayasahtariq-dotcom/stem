package com.stemtutor.feature.notes.generating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.model.PipelineStep
import com.stemtutor.core.data.model.UploadProgress
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.notes.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val UPLOAD_ID_ARG = "uploadId"

data class GeneratingUiState(val progress: UploadProgress? = null)

/** Shows each pipeline step as it finishes, from the app-wide [GenerationWatcher]. */
@HiltViewModel
class GeneratingViewModel @Inject constructor(
    private val watcher: GenerationWatcher,
    courses: CourseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val uploadId: String = checkNotNull(savedState[UPLOAD_ID_ARG])

    val state: StateFlow<GeneratingUiState> = watcher.progress
        .map { GeneratingUiState(it[uploadId]) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GeneratingUiState(watcher.progress.value[uploadId]))

    init {
        if (watcher.progress.value[uploadId] == null) {
            // e.g. the app was restarted: fetch once, then keep watching.
            viewModelScope.launch { courses.progress(uploadId).onSuccess(watcher::watch) }
        }
    }

    /** The user opened the result here, so the "ready" banner isn't needed. */
    fun onOpened() = watcher.dismiss(uploadId)
}

@Composable
fun GeneratingRoute(
    onOpenCourse: (courseId: String) -> Unit,
    onLeave: () -> Unit,
    isTeacher: Boolean,
    viewModel: GeneratingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GeneratingScreen(
        state = state,
        isTeacher = isTeacher,
        onOpen = { id ->
            viewModel.onOpened()
            onOpenCourse(id)
        },
        onLeave = onLeave,
    )
}

@Composable
fun stepLabel(step: PipelineStep, pages: Int): BilingualString = when (step) {
    PipelineStep.QUEUED -> bilingualStringResource(R.string.nt_step_queued)
    PipelineStep.READING -> bilingualStringResource(R.string.nt_step_reading, pages)
    PipelineStep.CHUNKING -> bilingualStringResource(R.string.nt_step_chunking)
    PipelineStep.OUTLINING -> bilingualStringResource(R.string.nt_step_outlining)
    PipelineStep.TERMS -> bilingualStringResource(R.string.nt_step_terms)
    PipelineStep.LESSON_1 -> bilingualStringResource(R.string.nt_step_lesson)
    PipelineStep.READY -> bilingualStringResource(R.string.nt_step_ready)
}

@Composable
fun GeneratingScreen(
    state: GeneratingUiState,
    isTeacher: Boolean,
    onOpen: (String) -> Unit,
    onLeave: () -> Unit,
    animate: Boolean = true,
) {
    val p = state.progress
    PromptScreen(
        bubble = bilingualStringResource(if (p?.ready == true) R.string.nt_step_ready else R.string.nt_gen_title),
        pose = when {
            p?.ready == true -> CowPose.Thumbs
            p?.failed == true || p?.needsReview == true -> CowPose.Sad
            else -> CowPose.Question
        },
        animate = animate,
        footer = {
            val courseId = p?.courseId
            if (p?.ready == true && courseId != null) {
                ChunkyButton(
                    if (isTeacher) R.string.nt_open_review else R.string.nt_open_course,
                    { onOpen(courseId) },
                    Modifier.fillMaxWidth().testTag("open_course"),
                )
            } else if (p?.finished != true) {
                BilingualText(R.string.nt_can_leave, size = BilingualTextSize.Small)
                ChunkyButton(R.string.nt_leave, onLeave, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
            } else {
                ChunkyButton(com.stemtutor.core.designsystem.R.string.ds_back, onLeave, Modifier.fillMaxWidth())
            }
        },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PipelineStep.entries.forEach { step ->
                val done = p != null && step in p.stepsDone && (step != p.current)
                val now = p?.current == step
                StepRow(stepLabel(step, p?.pageCount ?: 0), done = done || (p?.ready == true), now = now)
            }
        }
        when {
            p?.failed == true -> ErrorLine(bilingualStringResource(R.string.nt_gen_failed))
            p?.needsReview == true -> ErrorLine(bilingualStringResource(R.string.nt_gen_needs_review))
        }
    }
}

@Composable
private fun StepRow(label: BilingualString, done: Boolean, now: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (done || now) 1f else 0.5f)
            .clip(RoundedCornerShape(14.dp))
            .background(StemColors.Soft)
            .padding(10.dp)
            .testTag(if (done) "step_done" else "step_pending"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) StemColors.Green else if (now) StemColors.Gold else StemColors.Line),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else if (now) "…" else "", color = StemColors.White, fontWeight = FontWeight.ExtraBold)
        }
        BilingualText(label, size = BilingualTextSize.Small)
    }
}

@BilingualPreviews
@Composable
private fun GeneratingPreview() {
    StemTutorTheme {
        GeneratingScreen(
            GeneratingUiState(
                UploadProgress("u", "outlining", PipelineStep.entries.take(4), pageCount = 3),
            ),
            isTeacher = true, onOpen = {}, onLeave = {}, animate = false,
        )
    }
}
