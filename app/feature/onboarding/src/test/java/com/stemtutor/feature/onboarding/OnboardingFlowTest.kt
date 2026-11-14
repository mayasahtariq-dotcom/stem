package com.stemtutor.feature.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.auth.FakeAuthRepository
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.profile.FakeProfileRepository
import com.stemtutor.core.data.session.SessionStore
import com.stemtutor.core.network.ApiConfig
import com.stemtutor.feature.onboarding.choose.LanguageViewModel
import com.stemtutor.feature.onboarding.choose.RoleViewModel
import com.stemtutor.feature.onboarding.name.NameViewModel
import com.stemtutor.feature.onboarding.otp.OtpViewModel
import com.stemtutor.feature.onboarding.otp.extractOtp
import com.stemtutor.feature.onboarding.phone.PhoneViewModel
import com.stemtutor.feature.onboarding.splash.SplashUiState
import com.stemtutor.feature.onboarding.splash.SplashViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The full onboarding flow with the fake login, screen by screen through the real ViewModels:
 * phone → OTP → language → role → name → the chosen role's home. Then a returning user goes
 * straight to the home of the role they last used.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFlowTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appliedLocales = mutableListOf<LanguageMode>()

    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "s.preferences_pb") } }
    private val auth by lazy { FakeAuthRepository(store) }
    private val profiles by lazy { FakeProfileRepository(auth, store) }
    private val session by lazy { SessionStore(store) }
    private val languages by lazy { LanguageRepository(store) { appliedLocales += it } }
    private val draft = OnboardingDraft()
    private val config = ApiConfig(baseUrl = "http://localhost/", useRealApi = false, enableHttpLogging = false)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun <S> StateFlow<S>.await(nav: (S) -> OnboardingNav?): OnboardingNav =
        withTimeout(5_000) { first { nav(it) != null } }.let(nav)!!

    private suspend fun onboard(phone: String, mode: LanguageMode, role: Role): OnboardingNav {
        val phoneVm = PhoneViewModel(auth, draft)
        phoneVm.onPhoneChange(phone)
        phoneVm.sendOtp()
        assertEquals(OnboardingNav.Otp, phoneVm.state.await { it.navigation })

        val otpVm = OtpViewModel(auth, profiles, session, draft, config)
        assertEquals(true, otpVm.state.value.showTestCode)
        otpVm.onCodeChange(FakeAuthRepository.FAKE_OTP)
        assertEquals(OnboardingNav.Language, otpVm.state.await { it.navigation })

        val languageVm = LanguageViewModel(languages)
        languageVm.select(mode)
        languageVm.next()
        assertEquals(OnboardingNav.Role, languageVm.state.await { it.navigation })

        val roleVm = RoleViewModel(draft)
        roleVm.select(role)
        roleVm.next()
        assertEquals(OnboardingNav.Name, roleVm.state.await { it.navigation })

        val nameVm = NameViewModel(profiles, languages, session, draft)
        nameVm.onNameChange("Ananya")
        nameVm.save()
        return nameVm.state.await { it.navigation }
    }

    @Test
    fun studentOnboardingEndsOnStudentHome() = runBlocking {
        assertEquals(OnboardingNav.Home(Role.STUDENT), onboard("9876543210", LanguageMode.KANNADA, Role.STUDENT))
        val profile = profiles.me().getOrThrow()
        assertEquals(listOf(Role.STUDENT), profile.roles)
        assertEquals(LanguageMode.KANNADA, profile.languageMode)
        assertEquals("+919876543210", profile.phone)
        assertEquals(LanguageMode.KANNADA, appliedLocales.last())
    }

    @Test
    fun teacherOnboardingEndsOnTeacherHome() = runBlocking {
        assertEquals(OnboardingNav.Home(Role.TEACHER), onboard("9876543211", LanguageMode.HINDI, Role.TEACHER))
    }

    @Test
    fun parentOnboardingEndsOnParentHome() = runBlocking {
        assertEquals(OnboardingNav.Home(Role.PARENT), onboard("9876543212", LanguageMode.HINDI, Role.PARENT))
    }

    @Test
    fun returningUserGoesStraightToLastRoleHome() = runBlocking {
        onboard("9876543213", LanguageMode.HINDI, Role.TEACHER)
        val splash = SplashViewModel(auth, profiles, session)
        val go = withTimeout(5_000) { splash.state.first { it is SplashUiState.Go } } as SplashUiState.Go
        assertEquals(OnboardingNav.Home(Role.TEACHER), go.destination)
    }

    @Test
    fun signedOutUserSeesWelcome() = runBlocking {
        val splash = SplashViewModel(auth, profiles, session)
        assertEquals(SplashUiState.Welcome, withTimeout(5_000) { splash.state.first { it != SplashUiState.Loading } })
    }

    @Test
    fun wrongOtpShowsErrorAndStays() = runBlocking {
        draft.phone = "9876543214"
        val otpVm = OtpViewModel(auth, profiles, session, draft, config)
        otpVm.onCodeChange("000000")
        val state = withTimeout(5_000) { otpVm.state.first { it.error != null } }
        assertEquals(null, state.navigation)
        assertEquals("", state.code)
    }

    @Test
    fun otpIsReadFromSmsText() {
        assertEquals("123456", extractOtp("123456 is your Mooshala code"))
        assertEquals("482913", extractOtp("Your code: 482913. Do not share."))
        assertEquals(null, extractOtp("Call 9876543210 for help"))
    }
}
