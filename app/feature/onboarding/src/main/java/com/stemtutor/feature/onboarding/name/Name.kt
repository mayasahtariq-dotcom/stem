package com.stemtutor.feature.onboarding.name

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.data.session.SessionStore
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
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

data class NameUiState(
    val name: String = "",
    val saving: Boolean = false,
    val error: AppError? = null,
    val navigation: OnboardingNav? = null,
) {
    val canSave get() = name.isNotBlank() && !saving
}

/** Last onboarding step: saves name, language and role to the profile, then opens that home. */
@HiltViewModel
class NameViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val languages: LanguageRepository,
    private val session: SessionStore,
    private val draft: OnboardingDraft,
) : ViewModel() {
    private val _state = MutableStateFlow(NameUiState())
    val state: StateFlow<NameUiState> = _state.asStateFlow()

    fun onNameChange(name: String) = _state.update { it.copy(name = name.take(60), error = null) }

    fun save() {
        if (!_state.value.canSave) return
        val role = draft.role ?: Role.STUDENT
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val existingRoles = profiles.me().getOrNull()?.roles.orEmpty()
            profiles.save(_state.value.name, languages.current(), (existingRoles + role).distinct()).fold(
                onSuccess = {
                    session.setLastRole(role)
                    draft.clear()
                    _state.update { it.copy(saving = false, navigation = OnboardingNav.Home(role)) }
                },
                onFailure = { e -> _state.update { it.copy(saving = false, error = e.appError) } },
            )
        }
    }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun NameRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: NameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    NameScreen(state, viewModel::onNameChange, viewModel::save)
}

@Composable
fun NameScreen(state: NameUiState, onNameChange: (String) -> Unit, onSave: () -> Unit, animate: Boolean = true) {
    PromptScreen(
        bubble = bilingualStringResource(R.string.ob_name_ask),
        pose = CowPose.Heart,
        animate = animate,
        footer = { ChunkyButton(R.string.ob_done, onSave, Modifier.fillMaxWidth(), enabled = state.canSave) },
    ) {
        StemTextField(
            value = state.name,
            onValueChange = onNameChange,
            placeholder = bilingualStringResource(R.string.ob_name_placeholder),
            onDone = onSave,
        )
        state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
    }
}

@BilingualPreviews
@Composable
private fun NamePreview() {
    StemTutorTheme { NameScreen(NameUiState(name = "Ananya"), {}, {}, animate = false) }
}
