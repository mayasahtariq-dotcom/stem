package com.stemtutor.feature.parent

import androidx.lifecycle.SavedStateHandle
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.FakeAudioPlayer
import com.stemtutor.core.data.audio.FakeAudioRepository
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.family.FakeFamilyRepository
import com.stemtutor.core.data.model.Arrow
import com.stemtutor.core.data.model.ConsentStatus
import com.stemtutor.core.data.model.Profile
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.model.SpeechLanguage
import com.stemtutor.core.data.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private object Meena : ProfileRepository {
    override suspend fun me() = Result.success(Profile("p", true, "Meena", roles = listOf(Role.PARENT)))
    override suspend fun save(name: String, languageMode: LanguageMode, roles: List<Role>) = me()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ParentFlowTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun dashboardShowsTheFirstChildAndSwitches() {
        val audio = FakeAudioRepository()
        val vm = ParentHomeViewModel(Meena, FakeFamilyRepository(), ListenController(audio, FakeAudioPlayer()))
        vm.load()
        val s = vm.state.value
        assertEquals(listOf("Ananya", "Ravi"), s.children.map { it.name })
        assertEquals("k1", s.selectedId)
        assertEquals(Arrow.UP, s.dashboard!!.week.arrow)
        assertTrue(s.dashboard!!.hasActivity)

        vm.select("k2")
        val other = vm.state.value.dashboard!!
        assertEquals("Ravi", other.child.name)
        assertFalse(other.hasActivity) // the empty state
        assertEquals(Arrow.SAME, other.week.arrow)

        vm.listenSummary(LanguageMode.HINDI)
        assertEquals(SpeechLanguage.HINDI, audio.requested.first().second)
        assertEquals(SpeechLanguage.ENGLISH, audio.requested.last().second)
    }

    @Test
    fun arrowsPointTheRightWay() {
        assertEquals("▲", Arrow.UP.symbol())
        assertEquals("▼", Arrow.DOWN.symbol())
        assertEquals("=", Arrow.SAME.symbol())
    }

    @Test
    fun addChildChecksTheCodeThenLinks() {
        val vm = AddChildViewModel(FakeFamilyRepository())
        vm.type("zzzzzz")
        vm.check()
        assertTrue(vm.state.value.error != null)
        vm.type("kav-234")
        assertEquals("KAV234", vm.state.value.code)
        vm.check()
        assertEquals("Kavya", vm.state.value.preview!!.name)
        vm.link()
        assertEquals(ConsentStatus.PENDING, vm.state.value.linked!!.consent)
    }

    @Test
    fun approvingConsentFinishesTheScreen() {
        val vm = ChildViewModel(FakeFamilyRepository(), SavedStateHandle(mapOf(CHILD_ID_ARG to "k2")))
        assertEquals(ConsentStatus.PENDING, vm.state.value.child!!.consent)
        vm.approve()
        assertEquals(ConsentStatus.APPROVED, vm.state.value.child!!.consent)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun settingsAskBeforeRevokingOrDeleting() {
        val vm = ChildViewModel(FakeFamilyRepository(), SavedStateHandle(mapOf(CHILD_ID_ARG to "k1")))
        vm.ask(ChildAction.REVOKE)
        assertEquals(ChildAction.REVOKE, vm.state.value.confirm)
        vm.confirm()
        assertNull(vm.state.value.confirm)
        assertEquals(ConsentStatus.REVOKED, vm.state.value.child!!.consent)
        vm.ask(ChildAction.DELETE)
        vm.confirm()
        assertTrue(vm.state.value.done)
    }

    @Test
    fun linkCodesAreFoundInScannedText() {
        assertEquals("KAV234", LinkCodes.findIn("kav234"))
        assertEquals("KAV234", LinkCodes.findIn("mooshala link KAV234"))
        assertNull(LinkCodes.findIn("BI09-K4X2"))
        assertNull(LinkCodes.findIn("KAV2345"))
    }
}
