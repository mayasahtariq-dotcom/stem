package com.stemtutor.feature.student.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.classroom.JoinKeys
import com.stemtutor.core.data.model.ClassPreview
import com.stemtutor.core.data.model.StudentClassroom
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
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val JOIN_KEY_ARG = "key"

data class JoinClassUiState(
    /** Compact key, no dash: "BI09K4X2". */
    val key: String = "",
    val loadingPreview: Boolean = false,
    val preview: ClassPreview? = null,
    val joining: Boolean = false,
    val error: AppError? = null,
    val joined: StudentClassroom? = null,
)

/** Type or scan a key → preview the class → Join. Errors come back as codes, shown bilingually. */
@HiltViewModel
class JoinClassViewModel @Inject constructor(
    private val classrooms: ClassroomRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(JoinClassUiState())
    val state: StateFlow<JoinClassUiState> = _state.asStateFlow()
    private var previewJob: Job? = null

    init {
        savedState.get<String>(JOIN_KEY_ARG)?.let { onKeyChange(it) }
    }

    fun onKeyChange(raw: String) {
        val key = JoinKeys.sanitize(raw)
        if (key == _state.value.key && _state.value.preview != null) return
        _state.update { it.copy(key = key, preview = null, error = null) }
        previewJob?.cancel()
        if (JoinKeys.isComplete(key)) loadPreview(key)
    }

    private fun loadPreview(key: String) {
        _state.update { it.copy(loadingPreview = true) }
        previewJob = viewModelScope.launch {
            classrooms.preview(JoinKeys.format(key)).fold(
                onSuccess = { p -> _state.update { it.copy(loadingPreview = false, preview = p) } },
                onFailure = { e -> _state.update { it.copy(loadingPreview = false, error = e.appError) } },
            )
        }
    }

    fun retryPreview() = _state.value.key.takeIf(JoinKeys::isComplete)?.let(::loadPreview)

    fun join() {
        val current = _state.value
        if (current.preview == null || current.joining) return
        _state.update { it.copy(joining = true, error = null) }
        viewModelScope.launch {
            classrooms.join(JoinKeys.format(current.key)).fold(
                onSuccess = { c -> _state.update { it.copy(joining = false, joined = c) } },
                onFailure = { e -> _state.update { it.copy(joining = false, error = e.appError) } },
            )
        }
    }
}

@Composable
fun JoinClassRoute(
    onScan: () -> Unit,
    onJoined: () -> Unit,
    onBack: () -> Unit,
    scannedKey: String?,
    viewModel: JoinClassViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(scannedKey) { scannedKey?.let(viewModel::onKeyChange) }
    JoinClassScreen(state, viewModel::onKeyChange, onScan, viewModel::join, onJoined, onBack)
}

@Composable
fun JoinClassScreen(
    state: JoinClassUiState,
    onKeyChange: (String) -> Unit,
    onScan: () -> Unit,
    onJoin: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    animate: Boolean = true,
) {
    val joined = state.joined
    PromptScreen(
        bubble = if (joined != null) {
            bilingualStringResource(R.string.st_joined, joined.name)
        } else {
            bilingualStringResource(R.string.st_key_ask)
        },
        pose = when {
            joined != null -> CowPose.Thumbs
            state.error != null -> CowPose.Sad
            else -> CowPose.Question
        },
        animate = animate,
        footer = {
            when {
                joined != null -> ChunkyButton(DsR.string.ds_back, onDone, Modifier.fillMaxWidth())
                else -> {
                    ChunkyButton(
                        R.string.st_join,
                        onJoin,
                        Modifier.fillMaxWidth().testTag("join_button"),
                        enabled = state.preview != null && !state.joining,
                    )
                    ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        },
    ) {
        if (joined == null) {
            JoinKeyInput(
                value = state.key,
                onValueChange = onKeyChange,
                isError = state.error != null,
                enabled = !state.joining,
                accessibilityLabel = bilingualStringResource(R.string.st_key_label).regional,
                modifier = Modifier.testTag("join_key"),
            )
            ChunkyButton(R.string.st_scan, onScan, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
            state.error?.let { ErrorLine(bilingualStringResource(it.messageRes()), Modifier.testTag("join_error")) }
            state.preview?.let { PreviewCard(it) }
        }
    }
}

@Composable
private fun PreviewCard(p: ClassPreview) {
    val subject = bilingualStringResource(p.subject.nameRes())
    val grade = bilingualStringResource(DsR.string.grade_n, p.grade)
    val students = bilingualStringResource(DsR.string.students_n, p.studentCount)
    val teacher = bilingualStringResource(R.string.st_teacher_n, p.teacherName)
    val languageNames = p.allowedModes.map { bilingualStringResource(it.nameRes()) }
    val languages = bilingualStringResource(R.string.st_languages_n, languageNames.joinToString(", ") { it.regional })
    val languagesEn = bilingualStringResource(R.string.st_languages_n, languageNames.joinToString(", ") { it.english })
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StemColors.Cream)
            .padding(16.dp)
            .testTag("join_preview"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BilingualText(R.string.st_preview_title, size = BilingualTextSize.Small, regionalColor = StemColors.Brown, englishColor = StemColors.Brown)
        Text(p.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
        BilingualText(
            BilingualString(
                "${teacher.regional}\n${subject.regional} · ${grade.regional} · ${students.regional}\n${languages.regional}",
                "${teacher.english} · ${subject.english} · ${grade.english} · ${students.english} · ${languagesEn.english}",
            ),
            size = BilingualTextSize.Small,
        )
    }
}

@BilingualPreviews
@Composable
private fun JoinClassPreview() {
    StemTutorTheme {
        JoinClassScreen(
            JoinClassUiState(
                key = "BI09K4X2",
                preview = ClassPreview(
                    "1", "Class 9-A Science", "Mrs. Rao", com.stemtutor.core.data.model.Subject.BIOLOGY, 9, 38,
                    com.stemtutor.core.common.language.LanguageMode.entries,
                ),
            ),
            {}, {}, {}, {}, {}, animate = false,
        )
    }
}
