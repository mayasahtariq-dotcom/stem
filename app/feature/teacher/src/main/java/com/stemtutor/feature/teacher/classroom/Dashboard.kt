package com.stemtutor.feature.teacher.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.family.DashboardRepository
import com.stemtutor.core.data.model.ClassDashboard
import com.stemtutor.core.data.model.Concept
import com.stemtutor.core.data.model.Confusion
import com.stemtutor.core.data.model.CourseRef
import com.stemtutor.core.data.model.DashboardTiles
import com.stemtutor.core.data.model.LessonResultRow
import com.stemtutor.core.data.model.StudentDetail
import com.stemtutor.core.data.model.StudentRow
import com.stemtutor.core.data.model.Trend
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.model.WeakTopic
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.EmptyScreen
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.ProgressBar
import com.stemtutor.core.designsystem.component.TeacherHeader
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
import com.stemtutor.feature.teacher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val STUDENT_ID_ARG = "studentId"

private val Amber = Color(0xFFF2A93B)

/** Weak-topic bars (PRD M4): red below 50%, amber from 50 to 75%, green above 75%. */
fun accuracyColor(accuracy: Float): Color = when {
    accuracy < 0.5f -> StemColors.Red
    accuracy <= 0.75f -> Amber
    else -> StemColors.Green
}

fun percent(v: Float?): String = v?.let { "${(it * 100).toInt()}%" } ?: "–"

data class DashboardUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val dashboard: ClassDashboard? = null,
    val courseId: String? = null,
    val savingConsent: Boolean = false,
    val actionError: AppError? = null,
)

@HiltViewModel
class ClassDashboardViewModel @Inject constructor(
    private val dashboards: DashboardRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val classroomId: String = checkNotNull(savedState[CLASSROOM_ID_ARG])
    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value.dashboard == null) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            dashboards.classDashboard(classroomId, _state.value.courseId).fold(
                onSuccess = { d -> _state.update { it.copy(loading = false, dashboard = d, error = null) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
            )
        }
    }

    fun filter(courseId: String?) {
        _state.update { it.copy(courseId = courseId) }
        load()
    }

    fun setSchoolConsent(enabled: Boolean) {
        _state.update { it.copy(savingConsent = true, actionError = null) }
        viewModelScope.launch {
            dashboards.setSchoolConsent(classroomId, enabled).fold(
                onSuccess = { on -> _state.update { s -> s.copy(savingConsent = false, dashboard = s.dashboard?.copy(schoolConsent = on)) } },
                onFailure = { e -> _state.update { it.copy(savingConsent = false, actionError = e.appError) } },
            )
        }
    }
}

@Composable
fun ClassDashboardRoute(onStudent: (String) -> Unit, onBack: () -> Unit, viewModel: ClassDashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClassDashboardScreen(state, viewModel::load, viewModel::filter, viewModel::setSchoolConsent, onStudent, onBack)
}

/** The class at a glance (PRD M4): tiles, weak topics and the students list, behind first. */
@Composable
fun ClassDashboardScreen(
    state: DashboardUiState,
    onRetry: () -> Unit,
    onFilter: (String?) -> Unit,
    onSchoolConsent: (Boolean) -> Unit,
    onStudent: (String) -> Unit,
    onBack: () -> Unit,
    animate: Boolean = true,
) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        val d = state.dashboard
        val title = d?.name ?: bilingualStringResource(R.string.db_title).regional
        TeacherHeader(BilingualString(title, d?.joinKey ?: bilingualStringResource(R.string.db_title).english))
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            d == null -> EmptyScreen(onRetry, animate = animate)
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ConsentCard(d.schoolConsent, state.savingConsent, onSchoolConsent)
                state.actionError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
                if (d.courses.size > 1) CourseFilter(d.courses, state.courseId, onFilter)
                TileRow(d.tiles)
                BilingualText(R.string.db_weak_topics, size = BilingualTextSize.Large)
                if (d.weakTopics.isEmpty()) BilingualText(R.string.db_no_answers_yet, size = BilingualTextSize.Small)
                d.weakTopics.forEach { WeakTopicRow(it) }
                BilingualText(R.string.db_students, size = BilingualTextSize.Large)
                if (d.students.isEmpty()) BilingualText(R.string.db_no_students, size = BilingualTextSize.Small)
                d.students.forEach { StudentRowView(it) { onStudent(it.id) } }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
}

@Composable
private fun ConsentCard(on: Boolean, saving: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.White).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            BilingualText(R.string.db_school_consent)
            BilingualText(R.string.db_school_consent_help, size = BilingualTextSize.Small)
        }
        Switch(checked = on, onCheckedChange = onChange, enabled = !saving)
    }
}

@Composable
private fun CourseFilter(courses: List<CourseRef>, selected: String?, onFilter: (String?) -> Unit) {
    val mode = currentLanguageMode()
    val all = bilingualStringResource(R.string.db_all_courses)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(all, selected == null) { onFilter(null) }
        courses.forEach { c -> Chip(BilingualString(c.title.regional(mode), c.title.en), selected == c.id) { onFilter(c.id) } }
    }
}

@Composable
private fun Chip(text: BilingualString, on: Boolean, onClick: () -> Unit) {
    BilingualText(
        text,
        size = BilingualTextSize.Small,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (on) StemColors.BlueTint else StemColors.White)
            .border(2.dp, if (on) StemColors.Blue else StemColors.Line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun TileRow(t: DashboardTiles) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Sticker.Flame, "${t.activeThisWeek}/${t.studentCount}", bilingualStringResource(R.string.db_active), Modifier.weight(1f))
        Tile(Sticker.Target, percent(t.accuracy), bilingualStringResource(R.string.db_accuracy), Modifier.weight(1f))
        Tile(Sticker.Warn, "${t.behind}", bilingualStringResource(R.string.db_behind), Modifier.weight(1f), if (t.behind > 0) StemColors.Red else StemColors.Ink)
    }
}

@Composable
private fun Tile(icon: Sticker, value: String, label: BilingualString, modifier: Modifier, color: Color = StemColors.Ink) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(StemColors.White).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StickerIcon(icon, size = 24.dp)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        BilingualText(label, size = BilingualTextSize.Small)
    }
}

@Composable
private fun WeakTopicRow(w: WeakTopic) {
    val mode = currentLanguageMode()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BilingualText(BilingualString(w.name.regional(mode), w.name.en), modifier = Modifier.weight(1f))
            Text(percent(w.accuracy), fontWeight = FontWeight.Bold, color = accuracyColor(w.accuracy))
        }
        ProgressBar(w.accuracy, color = accuracyColor(w.accuracy))
        if (w.lowMasteryStudents > 0) {
            BilingualText(bilingualStringResource(R.string.db_low_mastery, w.lowMasteryStudents), size = BilingualTextSize.Small)
        }
    }
}

@Composable
private fun StudentRowView(s: StudentRow, onClick: () -> Unit) {
    val mode = currentLanguageMode()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (s.idle) StemColors.RedTint else StemColors.White)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (s.idle) StemColors.WrongText else StemColors.Ink)
            s.currentLesson?.let { BilingualText(BilingualString(it.regional(mode), it.en), size = BilingualTextSize.Small) }
            if (s.idle) {
                BilingualText(
                    bilingualStringResource(R.string.db_idle_days, s.idleDays),
                    size = BilingualTextSize.Small,
                    regionalColor = StemColors.WrongText,
                    englishColor = StemColors.WrongText,
                )
            } else if (s.overdue > 0) {
                BilingualText(
                    bilingualStringResource(R.string.db_overdue_n, s.overdue),
                    size = BilingualTextSize.Small,
                    regionalColor = StemColors.WrongText,
                    englishColor = StemColors.WrongText,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StickerIcon(Sticker.Flame, size = 18.dp)
                Text(" ${s.streak}", fontWeight = FontWeight.Bold, color = StemColors.Ink)
            }
            Text(percent(s.accuracy), fontWeight = FontWeight.Bold, color = s.accuracy?.let(::accuracyColor) ?: StemColors.Muted)
        }
    }
}

// ---- student detail ------------------------------------------------------------------------

data class StudentDetailUiState(val loading: Boolean = true, val error: AppError? = null, val detail: StudentDetail? = null)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val dashboards: DashboardRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val classroomId: String = checkNotNull(savedState[CLASSROOM_ID_ARG])
    private val studentId: String = checkNotNull(savedState[STUDENT_ID_ARG])
    private val _state = MutableStateFlow(StudentDetailUiState())
    val state: StateFlow<StudentDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            dashboards.studentDetail(classroomId, studentId).fold(
                onSuccess = { d -> _state.value = StudentDetailUiState(loading = false, detail = d) },
                onFailure = { e -> _state.value = StudentDetailUiState(loading = false, error = e.appError) },
            )
        }
    }
}

@Composable
fun StudentDetailRoute(onBack: () -> Unit, viewModel: StudentDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StudentDetailScreen(state, viewModel::load, onBack)
}

private fun Trend.res() = when (this) {
    Trend.IMPROVING -> R.string.db_trend_improving
    Trend.STEADY -> R.string.db_trend_steady
    Trend.NEEDS_PRACTICE -> R.string.db_trend_needs_practice
}

/** One student for their teacher: lessons, accuracy, streak and the twin's weak spots. Never doubts. */
@Composable
fun StudentDetailScreen(state: StudentDetailUiState, onRetry: () -> Unit, onBack: () -> Unit, animate: Boolean = true) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        val name = state.detail?.name.orEmpty()
        TeacherHeader(BilingualString(name, name.ifEmpty { " " }))
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            state.detail != null -> {
                val d = state.detail
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Tile(Sticker.Flame, "${d.streak}", bilingualStringResource(R.string.db_streak), Modifier.weight(1f))
                        Tile(Sticker.Book, "${d.lessonsCompleted}", bilingualStringResource(R.string.db_lessons_done), Modifier.weight(1f))
                        Box(Modifier.weight(1f)) {
                            Tile(Sticker.Chart, "", bilingualStringResource(d.trend.res()), Modifier.fillMaxWidth())
                        }
                    }
                    BilingualText(R.string.db_twin_title, size = BilingualTextSize.Large)
                    BilingualText(R.string.db_twin_help, size = BilingualTextSize.Small)
                    d.weakest.forEach { ConceptRow(it) }
                    d.confusions.forEach { ConfusionRow(it) }
                    BilingualText(R.string.db_lessons, size = BilingualTextSize.Large)
                    d.lessons.forEach { LessonRow(it) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
}

@Composable
private fun ConceptRow(c: Concept) {
    val mode = currentLanguageMode()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BilingualText(BilingualString(c.name.regional(mode), c.name.en), modifier = Modifier.weight(1f))
            Text(percent(c.mastery), fontWeight = FontWeight.Bold, color = accuracyColor(c.mastery))
        }
        ProgressBar(c.mastery, color = accuracyColor(c.mastery))
    }
}

@Composable
private fun ConfusionRow(c: Confusion) {
    val mode = currentLanguageMode()
    val text = BilingualString(
        bilingualStringResource(R.string.db_mixes_up, c.expected.regional(mode), c.chosen.regional(mode)).regional,
        bilingualStringResource(R.string.db_mixes_up, c.expected.en, c.chosen.en).english,
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.Cream).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StickerIcon(Sticker.Puzzle, size = 26.dp)
        BilingualText(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LessonRow(l: LessonResultRow) {
    val mode = currentLanguageMode()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StickerIcon(if (l.completed) Sticker.Star else Sticker.Book, size = 22.dp)
        BilingualText(BilingualString(l.title.regional(mode), l.title.en), modifier = Modifier.weight(1f))
        Text(percent(l.accuracy), fontWeight = FontWeight.Bold, color = l.accuracy?.let(::accuracyColor) ?: StemColors.Muted)
    }
}

private val previewDashboard = ClassDashboard(
    "c", "Class 9-A Science", "BI09-K4X2", false, emptyList(),
    DashboardTiles(6, 8, 0.72f, 2),
    listOf(WeakTopic("term", Tri("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್"), 0.38f, 21, 4)),
    listOf(
        StudentRow("s3", "Chetan", 0, Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"), 0.41f, 6, true, true, 1),
        StudentRow("s1", "Ananya", 6, Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"), 0.86f, 0, false, false, 0),
    ),
)

@BilingualPreviews
@Composable
private fun ClassDashboardPreview() {
    StemTutorTheme { ClassDashboardScreen(DashboardUiState(loading = false, dashboard = previewDashboard), {}, {}, {}, {}, {}, animate = false) }
}

@BilingualPreviews
@Composable
private fun StudentDetailPreview() {
    StemTutorTheme {
        StudentDetailScreen(
            StudentDetailUiState(
                loading = false,
                detail = StudentDetail(
                    "s3", "Chetan", 0, null, 0,
                    listOf(LessonResultRow("l1", Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"), false, 0.41f)),
                    listOf(Concept("term", Tri("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್"), 0.31f)),
                    listOf(Confusion(Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"), Tri("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್"), 3)),
                    Trend.NEEDS_PRACTICE,
                ),
            ),
            {}, {}, animate = false,
        )
    }
}
