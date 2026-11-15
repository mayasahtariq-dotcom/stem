package com.stemtutor.feature.parent

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.family.FamilyRepository
import com.stemtutor.core.data.model.Arrow
import com.stemtutor.core.data.model.Assignment
import com.stemtutor.core.data.model.AssignmentStatus
import com.stemtutor.core.data.model.Child
import com.stemtutor.core.data.model.ChildDashboard
import com.stemtutor.core.data.model.ConsentStatus
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Trend
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.BottomTabs
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.component.Tab
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

data class ParentHomeUiState(
    val name: String = "",
    val loading: Boolean = true,
    val error: AppError? = null,
    val children: List<Child> = emptyList(),
    val selectedId: String? = null,
    val dashboard: ChildDashboard? = null,
    val dashboardError: AppError? = null,
) {
    val selected: Child? get() = children.firstOrNull { it.id == selectedId }
}

/** The parent home is the dashboard (PRD M4): child chips, then this child's cards. */
@HiltViewModel
class ParentHomeViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val family: FamilyRepository,
    private val listen: ListenController,
) : ViewModel() {
    private val _state = MutableStateFlow(ParentHomeUiState())
    val state: StateFlow<ParentHomeUiState> = _state.asStateFlow()

    fun load() {
        if (_state.value.children.isEmpty()) _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val name = profiles.me().getOrNull()?.name.orEmpty()
            family.children().fold(
                onSuccess = { kids ->
                    val selected = _state.value.selectedId?.takeIf { id -> kids.any { it.id == id } } ?: kids.firstOrNull()?.id
                    _state.update { it.copy(name = name, loading = false, children = kids, selectedId = selected, error = null) }
                    selected?.let { loadDashboard(it) } ?: _state.update { it.copy(dashboard = null) }
                },
                onFailure = { e -> _state.update { it.copy(name = name, loading = false, error = e.appError) } },
            )
        }
    }

    private suspend fun loadDashboard(childId: String) {
        family.childDashboard(childId).fold(
            onSuccess = { d -> _state.update { it.copy(dashboard = d, dashboardError = null) } },
            onFailure = { e -> _state.update { it.copy(dashboard = null, dashboardError = e.appError) } },
        )
    }

    fun select(childId: String) {
        _state.update { it.copy(selectedId = childId, dashboard = null) }
        viewModelScope.launch { loadDashboard(childId) }
    }

    /** "Listen to summary": three sentences, in the parent's language then English. */
    fun listenSummary(mode: LanguageMode) {
        val summary = _state.value.dashboard?.summary ?: return
        viewModelScope.launch { listen.speak(summary, mode) }
    }

    override fun onCleared() {
        listen.stop()
    }
}

@Composable
fun ParentShell(
    profile: @Composable () -> Unit,
    onAddChild: () -> Unit = {},
    onConsent: (String) -> Unit = {},
    onChildSettings: (String) -> Unit = {},
    viewModel: ParentHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab(Sticker.Home, bilingualStringResource(DsR.string.ds_home)),
        Tab(Sticker.Duck, bilingualStringResource(DsR.string.ds_profile)),
    )
    val mode = currentLanguageMode()
    Scaffold(bottomBar = { BottomTabs(tabs, tab, { tab = it }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (tab == 0) {
                ParentHomeScreen(
                    state,
                    onRetry = viewModel::load,
                    onSelect = viewModel::select,
                    onAddChild = onAddChild,
                    onConsent = onConsent,
                    onSettings = onChildSettings,
                    onListen = { viewModel.listenSummary(mode) },
                )
            } else {
                profile()
            }
        }
    }
}

@Composable
fun ParentHomeScreen(
    state: ParentHomeUiState,
    onRetry: () -> Unit = {},
    onSelect: (String) -> Unit = {},
    onAddChild: () -> Unit = {},
    onConsent: (String) -> Unit = {},
    onSettings: (String) -> Unit = {},
    onListen: () -> Unit = {},
    animate: Boolean = true,
) {
    when {
        state.loading -> LoadingScreen(animate = animate)
        state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        state.children.isEmpty() -> NoChildren(state.name, onAddChild, animate)
        else -> Column(
            Modifier.fillMaxSize().background(StemColors.White).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BilingualText(bilingualStringResource(R.string.pa_hello, state.name), size = BilingualTextSize.Large)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.children.forEach { c -> ChildChip(c.name, c.id == state.selectedId) { onSelect(c.id) } }
                ChildChip("+", false, onAddChild)
            }
            val child = state.selected
            if (child != null && child.consent != ConsentStatus.APPROVED) {
                ConsentBanner(child) { onConsent(child.id) }
            }
            state.dashboardError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            state.dashboard?.let { DashboardCards(it, onListen) }
            child?.let { ChunkyButton(R.string.pa_child_settings, { onSettings(it.id) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost) }
        }
    }
}

@Composable
private fun NoChildren(name: String, onAddChild: () -> Unit, animate: Boolean) {
    Column(Modifier.fillMaxSize()) {
        val hello = bilingualStringResource(R.string.pa_hello, name)
        val add = bilingualStringResource(R.string.pa_no_children)
        PromptScreen(
            bubble = BilingualString("${hello.regional}\n${add.regional}", "${hello.english} ${add.english}"),
            pose = CowPose.Heart,
            animate = animate,
            modifier = Modifier.weight(1f).statusBarsPadding(),
        )
        ChunkyButton(R.string.pa_add_child, onAddChild, Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun ChildChip(name: String, on: Boolean, onClick: () -> Unit) {
    Text(
        name,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = StemColors.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) StemColors.BlueTint else StemColors.Soft)
            .border(2.dp, if (on) StemColors.Blue else StemColors.Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ConsentBanner(child: Child, onApprove: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.Cream).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val msg = if (child.consent == ConsentStatus.REVOKED) R.string.pa_consent_revoked else R.string.pa_consent_needed
        BilingualText(bilingualStringResource(msg, child.name))
        ChunkyButton(R.string.pa_review_consent, onApprove, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(StemColors.Soft).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private fun Trend.res() = when (this) {
    Trend.IMPROVING -> R.string.pa_improving
    Trend.STEADY -> R.string.pa_steady
    Trend.NEEDS_PRACTICE -> R.string.pa_needs_practice
}

/** The arrow shown next to lessons this week (PRD M4): compared with last week. */
fun Arrow.symbol() = when (this) {
    Arrow.UP -> "▲"
    Arrow.DOWN -> "▼"
    Arrow.SAME -> "="
}

fun Arrow.color(): Color = when (this) {
    Arrow.UP -> StemColors.Green
    Arrow.DOWN -> StemColors.Red
    Arrow.SAME -> StemColors.Muted
}

private fun Subject.nameRes() = when (this) {
    Subject.BIOLOGY -> DsR.string.subject_biology
    Subject.CHEMISTRY -> DsR.string.subject_chemistry
    Subject.PHYSICS -> DsR.string.subject_physics
}

@Composable
fun DashboardCards(d: ChildDashboard, onListen: () -> Unit) {
    val mode = currentLanguageMode()
    ChunkyButton(R.string.pa_listen_summary, onListen, Modifier.fillMaxWidth(), ChunkyButtonStyle.Cream)
    if (!d.hasActivity) {
        Card { BilingualText(bilingualStringResource(R.string.pa_no_activity, d.child.name)) }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StickerIcon(Sticker.Calendar, size = 26.dp)
            BilingualText(R.string.pa_this_week, size = BilingualTextSize.Large, modifier = Modifier.weight(1f))
            Text(d.week.arrow.symbol(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = d.week.arrow.color())
        }
        BilingualText(bilingualStringResource(R.string.pa_lessons_vs, d.week.lessonsThisWeek, d.week.lessonsLastWeek))
        BilingualText(bilingualStringResource(R.string.pa_minutes, d.week.minutes), size = BilingualTextSize.Small)
        BilingualText(bilingualStringResource(R.string.pa_streak, d.week.streak), size = BilingualTextSize.Small)
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StickerIcon(Sticker.Target, size = 26.dp)
            BilingualText(R.string.pa_accuracy, size = BilingualTextSize.Large, modifier = Modifier.weight(1f))
        }
        BilingualText(d.accuracy.trend.res(), regionalColor = if (d.accuracy.trend == Trend.NEEDS_PRACTICE) StemColors.WrongText else StemColors.CorrectText)
    }
    if (d.subjects.isNotEmpty()) {
        Card {
            BilingualText(R.string.pa_subjects, size = BilingualTextSize.Large)
            d.subjects.forEach { s ->
                BilingualText(bilingualStringResource(s.subject.nameRes()), size = BilingualTextSize.Small)
                s.strongest?.let { BilingualText(topic(R.string.pa_strongest, it, mode), size = BilingualTextSize.Small) }
                s.weakest?.let { BilingualText(topic(R.string.pa_weakest, it, mode), size = BilingualTextSize.Small) }
            }
        }
    }
    Card {
        BilingualText(R.string.pa_assignments, size = BilingualTextSize.Large)
        if (d.assignments.isEmpty()) BilingualText(R.string.pa_no_assignments, size = BilingualTextSize.Small)
        d.assignments.forEach { AssignmentRow(it) }
    }
    Card {
        BilingualText(R.string.pa_classes, size = BilingualTextSize.Large)
        d.classes.forEach { c ->
            Text("${c.name} · ${c.teacherName}", fontSize = 15.sp, color = StemColors.Ink)
        }
    }
}

@Composable
private fun topic(res: Int, t: Tri, mode: LanguageMode) = BilingualString(
    bilingualStringResource(res, t.regional(mode)).regional,
    bilingualStringResource(res, t.en).english,
)

@Composable
private fun AssignmentRow(a: Assignment) {
    val mode = currentLanguageMode()
    val (label, color) = when (a.status) {
        AssignmentStatus.DONE -> R.string.pa_done to StemColors.CorrectText
        AssignmentStatus.PENDING -> R.string.pa_pending to StemColors.Ink
        AssignmentStatus.OVERDUE -> R.string.pa_overdue to StemColors.Red
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            BilingualText(BilingualString(a.title.regional(mode), a.title.en), size = BilingualTextSize.Small)
            a.dueAt?.take(10)?.let { Text(it, fontSize = 13.sp, color = StemColors.Muted) }
        }
        BilingualText(label, size = BilingualTextSize.Small, regionalColor = color, englishColor = color)
    }
}

@BilingualPreviews
@Composable
private fun ParentHomePreview() {
    StemTutorTheme { ParentHomeScreen(ParentHomeUiState("Meena", loading = false), animate = false) }
}
