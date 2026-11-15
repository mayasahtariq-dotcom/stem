package com.stemtutor.feature.student.learn

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.CoursePath
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.HomeCourse
import com.stemtutor.core.data.model.NodeState
import com.stemtutor.core.data.model.PathNode
import com.stemtutor.core.data.model.PathUnit
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.EmptyScreen
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.TopStatsBar
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.bilingual
import com.stemtutor.feature.student.bilingualFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PATH_COURSE_ARG = "courseId"

/** Why a tap on the path did nothing: a locked lesson (and which one opens it), or still being made. */
sealed interface PathHint {
    data class Locked(val unlockedBy: Tri) : PathHint
    data object NotReady : PathHint
}

data class PathUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val courses: List<HomeCourse> = emptyList(),
    val selectedCourseId: String? = null,
    val path: CoursePath? = null,
    val stats: StudentStats = StudentStats.EMPTY,
    val hint: PathHint? = null,
)

/**
 * The Learn tab: each course as a zig-zag column of lesson nodes. Opened from a course card it
 * shows that course; as a tab it shows the course with the next lesson, with chips to switch.
 */
@HiltViewModel
class LessonPathViewModel @Inject constructor(
    private val learning: LearningRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val fixedCourseId: String? = savedState.get<String>(PATH_COURSE_ARG)
    private val _state = MutableStateFlow(PathUiState(selectedCourseId = fixedCourseId))
    val state: StateFlow<PathUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.path == null) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            learning.home().fold(
                onSuccess = { home ->
                    val selected = _state.value.selectedCourseId
                        ?: home.continueLesson?.courseId
                        ?: home.courses.firstOrNull()?.id
                    _state.update { it.copy(courses = home.courses, stats = home.stats, selectedCourseId = selected) }
                    if (selected == null) _state.update { it.copy(loading = false, path = null) } else loadPath(selected)
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
            )
        }
    }

    private suspend fun loadPath(courseId: String) {
        learning.path(courseId).fold(
            onSuccess = { p -> _state.update { it.copy(loading = false, path = p, error = null) } },
            onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
        )
    }

    fun select(courseId: String) {
        _state.update { it.copy(selectedCourseId = courseId, path = null, loading = true, hint = null) }
        viewModelScope.launch { loadPath(courseId) }
    }

    /** Returns the lesson to play, or shows why it can't be played yet. */
    fun tap(node: PathNode): String? {
        val hint = when {
            node.state == NodeState.LOCKED -> PathHint.Locked(node.unlockedBy ?: Tri.EMPTY)
            node.generationStatus != GenerationStatus.READY && node.generationStatus != GenerationStatus.NOT_STARTED -> PathHint.NotReady
            else -> null
        }
        _state.update { it.copy(hint = hint) }
        return if (hint == null) node.id else null
    }
}

@Composable
fun LessonPathRoute(
    onPlay: (lessonId: String) -> Unit,
    onTerms: (courseId: String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LessonPathViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LessonPathScreen(
        state = state,
        onRetry = viewModel::load,
        onSelect = viewModel::select,
        onTap = { node -> viewModel.tap(node)?.let(onPlay) },
        onTerms = { state.path?.courseId?.let(onTerms) },
        onBack = onBack,
    )
}

private fun Subject.accent(): Color = when (this) {
    Subject.BIOLOGY -> StemColors.GreenTint
    Subject.CHEMISTRY -> StemColors.Cream
    Subject.PHYSICS -> StemColors.BlueTint
}

private val ZIGZAG = listOf(0, 70, 110, 70, 0, -70, -110, -70)

@Composable
fun LessonPathScreen(
    state: PathUiState,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
    onTap: (PathNode) -> Unit,
    onTerms: () -> Unit,
    onBack: (() -> Unit)? = null,
    animate: Boolean = true,
) {
    Column(Modifier.fillMaxSize().background(StemColors.White)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                val back = bilingualStringResource(R.string.ln_back)
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "${back.regional} · ${back.english}", tint = StemColors.Ink)
                }
            }
            TopStatsBar(streakDays = state.stats.streak, xp = state.stats.xpTotal, hearts = state.stats.hearts, modifier = Modifier.weight(1f))
        }
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            state.path == null -> EmptyScreen(onRetry, messageRes = R.string.ln_no_courses, animate = animate)
            else -> PathBody(state, state.path, onSelect, onTap, onTerms)
        }
    }
}

@Composable
private fun PathBody(state: PathUiState, path: CoursePath, onSelect: (String) -> Unit, onTap: (PathNode) -> Unit, onTerms: () -> Unit) {
    val mode = currentLanguageMode()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.courses.size > 1) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.courses.forEach { c ->
                    val on = c.id == path.courseId
                    BilingualText(
                        c.title.bilingual(mode),
                        size = BilingualTextSize.Small,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) StemColors.BlueTint else StemColors.Soft)
                            .border(2.dp, if (on) StemColors.Blue else StemColors.Line, RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button) { onSelect(c.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        path.units.forEach { unit -> UnitBlock(path, unit, onTap) }
        state.hint?.let { hint ->
            val text = when (hint) {
                is PathHint.Locked -> bilingualFormat(R.string.ln_locked_hint, hint.unlockedBy.regional(mode), hint.unlockedBy.en)
                PathHint.NotReady -> bilingualStringResource(R.string.ln_getting_ready)
            }
            ErrorLine(text)
        }
        if (path.currentLessonId == null) BilingualText(R.string.ln_all_done)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChunkyButton(R.string.ln_revise, {}, Modifier.weight(1f), ChunkyButtonStyle.White, enabled = false)
            ChunkyButton(R.string.ln_terms, onTerms, Modifier.weight(1f), ChunkyButtonStyle.Cream)
        }
    }
}

@Composable
private fun UnitBlock(path: CoursePath, unit: PathUnit, onTap: (PathNode) -> Unit) {
    val mode = currentLanguageMode()
    val current = unit.lessons.firstOrNull { it.state == NodeState.CURRENT } ?: unit.lessons.lastOrNull { it.state == NodeState.DONE }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(path.subject.accent()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BilingualText(unit.title.bilingual(mode), size = BilingualTextSize.Large)
        current?.let { BilingualText(bilingualStringResource(R.string.ln_lesson_of, it.number, path.lessonCount), size = BilingualTextSize.Small) }
    }
    unit.lessons.forEachIndexed { i, node ->
        PathNodeView(node, Modifier.offset(x = ZIGZAG[i % ZIGZAG.size].dp), onTap)
    }
}

@Composable
private fun PathNodeView(node: PathNode, modifier: Modifier, onTap: (PathNode) -> Unit) {
    val mode = currentLanguageMode()
    val (fill, size) = when (node.state) {
        NodeState.DONE -> StemColors.Blue to 72.dp
        NodeState.CURRENT -> StemColors.KeyCream to 92.dp
        NodeState.LOCKED -> StemColors.Line to 72.dp
    }
    val title = node.title.bilingual(mode)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(fill)
                .border(4.dp, if (node.state == NodeState.CURRENT) StemColors.CreamEdge else Color.Transparent, CircleShape)
                .clickable(role = Role.Button) { onTap(node) }
                .semantics { contentDescription = "${node.number}. ${title.regional} · ${title.english}" },
            contentAlignment = Alignment.Center,
        ) {
            when (node.state) {
                NodeState.DONE -> Text("✓", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                NodeState.LOCKED -> StickerIcon(Sticker.Lock, size = 30.dp)
                NodeState.CURRENT -> Text("${node.number}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            }
        }
        if (node.state == NodeState.CURRENT) {
            BilingualText(title, size = BilingualTextSize.Small)
            BilingualText(R.string.ln_start, size = BilingualTextSize.Small, regionalColor = StemColors.BlueDark)
        }
    }
}

@BilingualPreviews
@Composable
private fun PathPreview() {
    val t = { en: String, hi: String, kn: String -> Tri(en, hi, kn) }
    val nodes = listOf(
        PathNode("1", 1, t("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"), NodeState.DONE, GenerationStatus.READY, 6),
        PathNode("2", 2, t("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"), NodeState.CURRENT, GenerationStatus.READY, 6),
        PathNode("3", 3, t("Energy in the cell", "कोशिका में ऊर्जा", "ಜೀವಕೋಶದಲ್ಲಿ ಶಕ್ತಿ"), NodeState.LOCKED, GenerationStatus.NOT_STARTED, 0),
    )
    val path = CoursePath(
        "c", t("The Cell", "कोशिका", "ಜೀವಕೋಶ"), Subject.BIOLOGY, false,
        listOf(PathUnit("u", t("Inside the cell", "कोशिका के अंदर", "ಜೀವಕೋಶದ ಒಳಗೆ"), nodes)), 1, 3, "2",
    )
    StemTutorTheme {
        LessonPathScreen(PathUiState(loading = false, path = path), {}, {}, {}, {}, animate = false)
    }
}
