package com.stemtutor.feature.onboarding.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.model.Profile
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.data.session.SessionStore
import com.stemtutor.core.designsystem.component.AnswerOption
import com.stemtutor.core.designsystem.component.AnswerOptionState
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.R
import com.stemtutor.feature.onboarding.choose.ROLE_OPTIONS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val currentRole: Role? = null,
    val busy: Boolean = false,
    val loadError: AppError? = null,
    val actionError: AppError? = null,
    val navigation: OnboardingNav? = null,
)

/** Name and phone, language toggle, role switcher, add a role, sign out. Shared by all roles. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val languages: LanguageRepository,
    private val session: SessionStore,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            profiles.me().fold(
                onSuccess = { p -> _state.update { it.copy(loading = false, profile = p, currentRole = session.lastRole() ?: p.roles.firstOrNull()) } },
                onFailure = { e -> _state.update { it.copy(loading = false, loadError = e.appError) } },
            )
        }
    }

    private fun saveProfile(languageMode: LanguageMode? = null, roles: List<Role>? = null, then: suspend (Profile) -> Unit = {}) {
        val p = _state.value.profile ?: return
        _state.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            profiles.save(p.name, languageMode ?: p.languageMode, roles ?: p.roles).fold(
                onSuccess = { saved ->
                    _state.update { it.copy(busy = false, profile = saved) }
                    then(saved)
                },
                onFailure = { e -> _state.update { it.copy(busy = false, actionError = e.appError) } },
            )
        }
    }

    fun setLanguage(mode: LanguageMode) {
        if (mode == _state.value.profile?.languageMode) return
        saveProfile(languageMode = mode) { languages.setLanguageMode(mode) }
    }

    fun switchTo(role: Role) {
        viewModelScope.launch {
            session.setLastRole(role)
            _state.update { it.copy(currentRole = role, navigation = OnboardingNav.Home(role)) }
        }
    }

    fun addRole(role: Role) {
        val p = _state.value.profile ?: return
        saveProfile(roles = (p.roles + role).distinct()) { switchTo(role) }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            session.clear()
            _state.update { it.copy(navigation = OnboardingNav.Welcome) }
        }
    }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun ProfileRoute(
    onNavigate: (OnboardingNav) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    extraContent: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    ProfileScreen(
        state = state,
        onRetry = viewModel::load,
        onLanguage = viewModel::setLanguage,
        onSwitch = viewModel::switchTo,
        onAddRole = viewModel::addRole,
        onSignOut = viewModel::signOut,
        extraContent = extraContent,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onLanguage: (LanguageMode) -> Unit,
    onSwitch: (Role) -> Unit,
    onAddRole: (Role) -> Unit,
    onSignOut: () -> Unit,
    animate: Boolean = true,
    extraContent: @Composable () -> Unit = {},
) {
    val profile = state.profile
    when {
        state.loading -> return LoadingScreen(animate = animate)
        profile == null -> return ErrorScreen(onRetry = onRetry, messageRes = (state.loadError ?: AppError.Unknown).messageRes(), animate = animate)
    }
    requireNotNull(profile)
    Column(
        Modifier
            .fillMaxSize()
            .background(StemColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(StemColors.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CowMascot(CowPose.Wave, width = 92.dp, animate = animate)
            Column {
                Text(profile.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
                Text(profile.phone, fontSize = 16.sp, color = StemColors.Muted)
            }
        }

        BilingualText(R.string.profile_language, size = BilingualTextSize.Large)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                LanguageMode.HINDI to BilingualString("हिंदी", "Hindi + English"),
                LanguageMode.KANNADA to BilingualString("ಕನ್ನಡ", "Kannada + English"),
            ).forEach { (mode, label) ->
                AnswerOption(
                    text = label,
                    state = if (profile.languageMode == mode) AnswerOptionState.Selected else AnswerOptionState.Default,
                    onClick = { onLanguage(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        BilingualText(R.string.profile_roles, size = BilingualTextSize.Large)
        ROLE_OPTIONS.forEach { option ->
            val has = option.role in profile.roles
            val current = option.role == state.currentRole
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StemColors.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StickerIcon(option.icon, size = 30.dp)
                BilingualText(option.title, modifier = Modifier.weight(1f))
                when {
                    current -> BilingualText(
                        R.string.profile_current,
                        size = BilingualTextSize.Small,
                        regionalColor = StemColors.CorrectText,
                        englishColor = StemColors.CorrectText,
                    )
                    has -> ChunkyButton(R.string.profile_switch, { onSwitch(option.role) }, style = ChunkyButtonStyle.White, enabled = !state.busy)
                    else -> ChunkyButton(R.string.profile_add_role, { onAddRole(option.role) }, style = ChunkyButtonStyle.White, enabled = !state.busy)
                }
            }
        }
        state.actionError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        extraContent()
        ChunkyButton(R.string.profile_sign_out, onSignOut, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
    }
}

@BilingualPreviews
@Composable
private fun ProfilePreview() {
    StemTutorTheme {
        ProfileScreen(
            state = ProfileUiState(
                loading = false,
                profile = Profile("1", true, "Ananya", "+919876543210", listOf(Role.STUDENT), LanguageMode.HINDI),
                currentRole = Role.STUDENT,
            ),
            onRetry = {}, onLanguage = {}, onSwitch = {}, onAddRole = {}, onSignOut = {}, animate = false,
        )
    }
}
