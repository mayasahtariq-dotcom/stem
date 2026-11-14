package com.stemtutor.app.gallery

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.common.language.LanguageRepository
import com.stemtutor.core.data.health.BackendStatus
import com.stemtutor.core.data.health.FakeHealthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {
    @get:Rule val tmp = TemporaryFolder()

    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val applied = mutableListOf<LanguageMode>()
    private lateinit var languageRepository: LanguageRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val store = PreferenceDataStoreFactory.create(scope = storeScope) {
            File(tmp.root, "settings.preferences_pb")
        }
        languageRepository = LanguageRepository(store) { applied += it }
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun switchingLanguageUpdatesStateAndAppliesLocale() = runBlocking {
        val vm = GalleryViewModel(languageRepository, FakeHealthRepository())
        withTimeout(5_000) { vm.state.first { it.languageMode == LanguageMode.HINDI } }

        vm.setLanguageMode(LanguageMode.KANNADA)

        withTimeout(5_000) { vm.state.first { it.languageMode == LanguageMode.KANNADA } }
        assertEquals(listOf(LanguageMode.KANNADA), applied)
    }

    @Test
    fun backendStatusReflectsRepository() = runTest {
        val ok = GalleryViewModel(
            languageRepository,
            FakeHealthRepository(Result.success(BackendStatus(true, "0.1.0"))),
        )
        assertEquals(BackendState.Connected("0.1.0"), ok.state.value.backend)

        val down = GalleryViewModel(
            languageRepository,
            FakeHealthRepository(Result.failure(IOException("offline"))),
        )
        assertEquals(BackendState.Unreachable, down.state.value.backend)
    }

    @Test
    fun selectingOptionMarksIt() {
        val vm = GalleryViewModel(languageRepository, FakeHealthRepository())
        vm.selectOption(1)
        assertEquals(1, vm.state.value.selectedOption)
    }
}
