package com.stemtutor.feature.onboarding.otp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.CodeBoxes
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.onboarding.OnboardingDraft
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.R
import com.stemtutor.feature.onboarding.homeRoleFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30

data class OtpUiState(
    val phone: String = "",
    val code: String = "",
    val verifying: Boolean = false,
    val resendIn: Int = RESEND_SECONDS,
    val error: AppError? = null,
    val showTestCode: Boolean = false,
    val navigation: OnboardingNav? = null,
) {
    val canVerify get() = code.length == OTP_LENGTH && !verifying
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val session: SessionStore,
    private val draft: OnboardingDraft,
    config: ApiConfig,
) : ViewModel() {
    private val _state = MutableStateFlow(OtpUiState(phone = draft.phone, showTestCode = !config.useRealApi))
    val state: StateFlow<OtpUiState> = _state.asStateFlow()
    private var countdown: Job? = null

    init {
        startCountdown()
    }

    private fun startCountdown() {
        countdown?.cancel()
        _state.update { it.copy(resendIn = RESEND_SECONDS) }
        countdown = viewModelScope.launch {
            while (_state.value.resendIn > 0) {
                delay(1_000)
                _state.update { it.copy(resendIn = it.resendIn - 1) }
            }
        }
    }

    fun onCodeChange(code: String) {
        val digits = code.filter(Char::isDigit).take(OTP_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
        if (digits.length == OTP_LENGTH) verify()
    }

    fun verify() {
        val current = _state.value
        if (!current.canVerify) return
        _state.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch {
            val verified = auth.verifyOtp(current.phone, current.code)
            val error = verified.exceptionOrNull()?.appError
            if (error != null) {
                _state.update { it.copy(verifying = false, error = error, code = "") }
                return@launch
            }
            profiles.me().fold(
                onSuccess = { profile ->
                    val role = homeRoleFor(profile, session.lastRole())
                    val next = if (profile.registered && role != null) OnboardingNav.Home(role) else OnboardingNav.Language
                    _state.update { it.copy(verifying = false, navigation = next) }
                },
                onFailure = { e -> _state.update { it.copy(verifying = false, error = e.appError) } },
            )
        }
    }

    fun resend() {
        if (_state.value.resendIn > 0) return
        viewModelScope.launch {
            auth.sendOtp(_state.value.phone).onFailure { e -> _state.update { it.copy(error = e.appError) } }
        }
        startCountdown()
    }

    fun changeNumber() = _state.update { it.copy(navigation = OnboardingNav.Phone) }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun OtpRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: OtpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    SmsCodeListener(onCode = viewModel::onCodeChange)
    OtpScreen(state, viewModel::onCodeChange, viewModel::verify, viewModel::resend, viewModel::changeNumber)
}

@Composable
fun OtpScreen(
    state: OtpUiState,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeNumber: () -> Unit,
    animate: Boolean = true,
) {
    PromptScreen(
        bubble = bilingualStringResource(R.string.ob_otp_ask, "+91 ${state.phone}"),
        pose = CowPose.Thumbs,
        animate = animate,
        footer = {
            ChunkyButton(R.string.ob_verify, onVerify, Modifier.fillMaxWidth(), enabled = state.canVerify)
            if (state.resendIn > 0) {
                BilingualText(
                    text = bilingualStringResource(R.string.ob_resend_in, state.resendIn),
                    size = BilingualTextSize.Small,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ChunkyButton(R.string.ob_resend, onResend, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
            }
            ChunkyButton(R.string.ob_change_number, onChangeNumber, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        },
    ) {
        CodeBoxes(
            value = state.code,
            onValueChange = onCodeChange,
            length = OTP_LENGTH,
            keyboardType = KeyboardType.NumberPassword,
            isError = state.error != null,
            enabled = !state.verifying,
            accessibilityLabel = bilingualStringResource(R.string.ob_otp_label).regional,
            onDone = onVerify,
        )
        state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        if (state.showTestCode) {
            BilingualText(
                textRes = R.string.ob_test_code,
                size = BilingualTextSize.Small,
                regionalColor = StemColors.Brown,
                englishColor = StemColors.Brown,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@BilingualPreviews
@Composable
private fun OtpPreview() {
    StemTutorTheme { OtpScreen(OtpUiState(phone = "9876543210", code = "123", resendIn = 21, showTestCode = true), {}, {}, {}, {}, animate = false) }
}
