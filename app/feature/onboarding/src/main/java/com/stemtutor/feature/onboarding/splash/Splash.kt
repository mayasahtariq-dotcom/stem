package com.stemtutor.feature.onboarding.splash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.data.session.SessionStore
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.R
import com.stemtutor.feature.onboarding.homeRoleFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    data object Loading : SplashUiState
    /** Not signed in: show the welcome with a start button. */
    data object Welcome : SplashUiState
    data class Error(val error: AppError) : SplashUiState
    data class Go(val destination: OnboardingNav) : SplashUiState
}

/** Decides where the app opens: welcome, the rest of onboarding, or the last role's home. */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    init {
        route()
    }

    fun route() {
        _state.value = SplashUiState.Loading
        viewModelScope.launch {
            if (auth.currentSession() == null) {
                _state.value = SplashUiState.Welcome
                return@launch
            }
            profiles.me().fold(
                onSuccess = { profile ->
                    val role = homeRoleFor(profile, session.lastRole())
                    _state.value = SplashUiState.Go(
                        if (profile.registered && role != null) OnboardingNav.Home(role) else OnboardingNav.Language,
                    )
                },
                onFailure = { e ->
                    val error = e.appError
                    _state.value = if (error == AppError.Api(ApiErrorCode.UNAUTHORIZED)) {
                        auth.signOut()
                        SplashUiState.Welcome
                    } else {
                        SplashUiState.Error(error)
                    }
                },
            )
        }
    }
}

@Composable
fun SplashRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: SplashViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { (state as? SplashUiState.Go)?.let { onNavigate(it.destination) } }
    SplashScreen(state = state, onStart = { onNavigate(OnboardingNav.Phone) }, onRetry = viewModel::route)
}

@Composable
fun SplashScreen(state: SplashUiState, onStart: () -> Unit, onRetry: () -> Unit, animate: Boolean = true) {
    if (state is SplashUiState.Error) {
        ErrorScreen(onRetry = onRetry, messageRes = state.error.messageRes(), animate = animate)
        return
    }
    PromptScreen(
        bubble = null,
        pose = CowPose.Wave,
        animate = animate,
        footer = {
            if (state == SplashUiState.Welcome) {
                ChunkyButton(R.string.ob_start, onStart, Modifier.fillMaxWidth())
                BilingualText(
                    textRes = R.string.ob_terms,
                    size = BilingualTextSize.Small,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val name = bilingualStringResource(R.string.ob_app_name)
            Text(name.regional, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = StemColors.Ink)
            Text(name.english, style = MaterialTheme.typography.titleLarge, color = StemColors.Muted)
        }
        BilingualText(R.string.ob_tagline, size = BilingualTextSize.Large, textAlign = TextAlign.Center)
    }
}

@BilingualPreviews
@Composable
private fun SplashPreview() {
    StemTutorTheme { SplashScreen(SplashUiState.Welcome, {}, {}, animate = false) }
}
