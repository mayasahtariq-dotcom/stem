package com.stemtutor.feature.onboarding.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
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

data class PhoneUiState(
    val phone: String = "",
    val sending: Boolean = false,
    val error: AppError? = null,
    val navigation: OnboardingNav? = null,
) {
    val canSend get() = phone.length == 10 && phone.first() in '6'..'9' && !sending
}

@HiltViewModel
class PhoneViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val draft: OnboardingDraft,
) : ViewModel() {
    private val _state = MutableStateFlow(PhoneUiState(phone = draft.phone))
    val state: StateFlow<PhoneUiState> = _state.asStateFlow()

    fun onPhoneChange(raw: String) {
        _state.update { it.copy(phone = raw.filter(Char::isDigit).take(10), error = null) }
    }

    fun sendOtp() {
        val phone = _state.value.phone
        if (!_state.value.canSend) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            auth.sendOtp(phone).fold(
                onSuccess = {
                    draft.phone = phone
                    _state.update { it.copy(sending = false, navigation = OnboardingNav.Otp) }
                },
                onFailure = { e -> _state.update { it.copy(sending = false, error = e.appError) } },
            )
        }
    }

    fun onNavigated() = _state.update { it.copy(navigation = null) }
}

@Composable
fun PhoneRoute(onNavigate: (OnboardingNav) -> Unit, viewModel: PhoneViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.navigation) {
        state.navigation?.let {
            viewModel.onNavigated()
            onNavigate(it)
        }
    }
    PhoneScreen(state, viewModel::onPhoneChange, viewModel::sendOtp)
}

@Composable
fun PhoneScreen(state: PhoneUiState, onPhoneChange: (String) -> Unit, onSend: () -> Unit, animate: Boolean = true) {
    PromptScreen(
        bubble = bilingualStringResource(R.string.ob_phone_ask),
        pose = CowPose.Question,
        animate = animate,
        footer = {
            ChunkyButton(R.string.ob_send_otp, onSend, Modifier.fillMaxWidth(), enabled = state.canSend)
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("+91", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            StemTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                placeholder = bilingualStringResource(R.string.ob_phone_placeholder),
                keyboardType = KeyboardType.Phone,
                capitalization = KeyboardCapitalization.None,
                maxLength = 10,
                onDone = onSend,
            )
        }
        state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
    }
}

@BilingualPreviews
@Composable
private fun PhonePreview() {
    StemTutorTheme { PhoneScreen(PhoneUiState(phone = "98765"), {}, {}, animate = false) }
}
