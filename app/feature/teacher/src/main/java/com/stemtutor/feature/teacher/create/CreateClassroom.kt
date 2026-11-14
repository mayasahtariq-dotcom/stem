package com.stemtutor.feature.teacher.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.model.GRADES
import com.stemtutor.core.data.model.NewClassroom
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.component.TeacherHeader
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.teacher.R
import com.stemtutor.feature.teacher.icon
import com.stemtutor.feature.teacher.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

data class CreateClassroomUiState(
    val name: String = "",
    val subject: Subject = Subject.BIOLOGY,
    val grade: Int = 9,
    val modes: Set<LanguageMode> = LanguageMode.entries.toSet(),
    val saving: Boolean = false,
    val showValidation: Boolean = false,
    val error: AppError? = null,
    val createdId: String? = null,
) {
    val nameMissing get() = name.isBlank()
    val modesMissing get() = modes.isEmpty()
    val valid get() = !nameMissing && !modesMissing
}

@HiltViewModel
class CreateClassroomViewModel @Inject constructor(private val classrooms: ClassroomRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateClassroomUiState())
    val state: StateFlow<CreateClassroomUiState> = _state.asStateFlow()

    fun onName(v: String) = _state.update { it.copy(name = v.take(60), error = null) }
    fun onSubject(s: Subject) = _state.update { it.copy(subject = s) }
    fun onGrade(g: Int) = _state.update { it.copy(grade = g.coerceIn(GRADES)) }
    fun toggleMode(m: LanguageMode) = _state.update {
        it.copy(modes = if (m in it.modes) it.modes - m else it.modes + m)
    }

    fun save() {
        val s = _state.value
        if (!s.valid) {
            _state.update { it.copy(showValidation = true) }
            return
        }
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            classrooms.create(NewClassroom(s.name, s.subject, s.grade, LanguageMode.entries.filter { it in s.modes })).fold(
                onSuccess = { c -> _state.update { it.copy(saving = false, createdId = c.id) } },
                onFailure = { e -> _state.update { it.copy(saving = false, error = e.appError) } },
            )
        }
    }

    fun onNavigated() = _state.update { it.copy(createdId = null) }
}

@Composable
fun CreateClassroomRoute(onCreated: (String) -> Unit, onBack: () -> Unit, viewModel: CreateClassroomViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) {
        state.createdId?.let {
            viewModel.onNavigated()
            onCreated(it)
        }
    }
    CreateClassroomScreen(state, viewModel::onName, viewModel::onSubject, viewModel::onGrade, viewModel::toggleMode, viewModel::save, onBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateClassroomScreen(
    state: CreateClassroomUiState,
    onName: (String) -> Unit,
    onSubject: (Subject) -> Unit,
    onGrade: (Int) -> Unit,
    onToggleMode: (LanguageMode) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground).imePadding()) {
        TeacherHeader(bilingualStringResource(R.string.te_create))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BilingualText(R.string.te_name_label, size = BilingualTextSize.Small)
            StemTextField(
                value = state.name,
                onValueChange = onName,
                placeholder = bilingualStringResource(R.string.te_name_placeholder),
                capitalization = KeyboardCapitalization.Sentences,
                centered = false,
            )
            if (state.showValidation && state.nameMissing) ErrorLine(bilingualStringResource(R.string.te_name_required))

            BilingualText(R.string.te_subject, size = BilingualTextSize.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Subject.entries.forEach { s ->
                    ChoiceCard(selected = state.subject == s, onClick = { onSubject(s) }, modifier = Modifier.weight(1f)) {
                        StickerIcon(s.icon(), size = 28.dp)
                        BilingualText(s.nameRes(), size = BilingualTextSize.Small)
                    }
                }
            }

            BilingualText(R.string.te_grade, size = BilingualTextSize.Small)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GRADES.forEach { g ->
                    ChoiceCard(selected = state.grade == g, onClick = { onGrade(g) }, modifier = Modifier.width(64.dp)) {
                        Text("$g", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
                    }
                }
            }

            BilingualText(R.string.te_languages, size = BilingualTextSize.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageMode.entries.forEach { m ->
                    ChoiceCard(selected = m in state.modes, onClick = { onToggleMode(m) }, modifier = Modifier.weight(1f)) {
                        BilingualText(m.nameRes(), size = BilingualTextSize.Small)
                    }
                }
            }
            if (state.showValidation && state.modesMissing) ErrorLine(bilingualStringResource(R.string.te_pick_language))
            state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        }
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChunkyButton(R.string.te_save, onSave, Modifier.fillMaxWidth(), enabled = !state.saving)
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
}

@BilingualPreviews
@Composable
private fun CreateClassroomPreview() {
    StemTutorTheme {
        CreateClassroomScreen(CreateClassroomUiState(name = "Class 9-A Science"), {}, {}, {}, {}, {}, {})
    }
}
