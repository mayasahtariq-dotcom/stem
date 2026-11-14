package com.stemtutor.feature.onboarding.choose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.designsystem.component.BigChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.onboarding.OnboardingDraft
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

// ---- Language ---------------------------------------------------------------------------

data class LanguageUiState(
    val selected: LanguageMode? = null,
    val navigation: OnboardingNav? = null,
)

/** Picking a language applies it at once (the per-app locale), then moves on to the role. */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languages: LanguageRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LanguageUiState())
    val state: StateFlow<LanguageUiState> = _state.asStateFlow()

    fun select(mode: LanguageMode) = _state.update { it.copy(selected = mode) }

    fun next() {
        val mode = _state.value.selected ?: return
        viewModelScope.launch {
            languages.setLanguageMode(mode)
            _state.update { it.copy(navigation = OnboardingNav.Role) }
        }
    }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun LanguageRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: LanguageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    LanguageScreen(state, viewModel::select, viewModel::next)
}

/**
 * The very first choice, so its texts are fixed in all three languages rather than following a
 * locale the user hasn't picked yet.
 */
@Composable
fun LanguageScreen(
    state: LanguageUiState,
    onSelect: (LanguageMode) -> Unit,
    onNext: () -> Unit,
    animate: Boolean = true,
) {
    PromptScreen(
        bubble = BilingualString(
            "तुम किस भाषा में सहज हो?\nನಿಮಗೆ ಯಾವ ಭಾಷೆ ಸುಲಭ?",
            "Which language are you comfortable with?",
        ),
        pose = CowPose.Question,
        animate = animate,
        footer = {
            ChunkyButton(
                label = BilingualString("आगे · ಮುಂದೆ", "Next"),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selected != null,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BigChoiceCard(
                title = "हिंदी",
                subtitle = BilingualString("हिंदी पहले, नीचे English", "Hindi + English"),
                selected = state.selected == LanguageMode.HINDI,
                onClick = { onSelect(LanguageMode.HINDI) },
            )
            BigChoiceCard(
                title = "ಕನ್ನಡ",
                subtitle = BilingualString("ಕನ್ನಡ ಮೊದಲು, ಕೆಳಗೆ English", "Kannada + English"),
                selected = state.selected == LanguageMode.KANNADA,
                onClick = { onSelect(LanguageMode.KANNADA) },
            )
        }
    }
}

// ---- Role -------------------------------------------------------------------------------

data class RoleUiState(val selected: Role? = null, val navigation: OnboardingNav? = null)

@HiltViewModel
class RoleViewModel @Inject constructor(private val draft: OnboardingDraft) : ViewModel() {
    private val _state = MutableStateFlow(RoleUiState(selected = draft.role))
    val state: StateFlow<RoleUiState> = _state.asStateFlow()

    fun select(role: Role) = _state.update { it.copy(selected = role) }

    fun next() {
        val role = _state.value.selected ?: return
        draft.role = role
        _state.update { it.copy(navigation = OnboardingNav.Name) }
    }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun RoleRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: RoleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    RoleScreen(state, viewModel::select, viewModel::next)
}

data class RoleOption(val role: Role, val icon: Sticker, val title: Int, val subtitle: Int)

val ROLE_OPTIONS = listOf(
    RoleOption(Role.STUDENT, Sticker.Backpack, DsR.string.role_student, R.string.ob_role_student_sub),
    RoleOption(Role.TEACHER, Sticker.Teacher, DsR.string.role_teacher, R.string.ob_role_teacher_sub),
    RoleOption(Role.PARENT, Sticker.People, DsR.string.role_parent, R.string.ob_role_parent_sub),
)

@Composable
fun RoleScreen(state: RoleUiState, onSelect: (Role) -> Unit, onNext: () -> Unit, animate: Boolean = true) {
    PromptScreen(
        bubble = bilingualStringResource(R.string.ob_role_ask),
        pose = CowPose.Question,
        animate = animate,
        footer = {
            ChunkyButton(R.string.ob_next, onNext, Modifier.fillMaxWidth(), enabled = state.selected != null)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ROLE_OPTIONS.forEach { option ->
                val title = bilingualStringResource(option.title)
                val sub = bilingualStringResource(option.subtitle)
                BigChoiceCard(
                    title = title.regional,
                    subtitle = BilingualString(sub.regional, "${title.english} · ${sub.english}"),
                    selected = state.selected == option.role,
                    onClick = { onSelect(option.role) },
                    icon = option.icon,
                )
            }
        }
    }
}

@BilingualPreviews
@Composable
private fun LanguagePreview() {
    StemTutorTheme { LanguageScreen(LanguageUiState(selected = LanguageMode.HINDI), {}, {}, animate = false) }
}

@BilingualPreviews
@Composable
private fun RolePreview() {
    StemTutorTheme { RoleScreen(RoleUiState(selected = Role.TEACHER), {}, {}, animate = false) }
}
