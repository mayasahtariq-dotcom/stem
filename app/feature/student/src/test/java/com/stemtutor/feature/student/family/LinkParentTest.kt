package com.stemtutor.feature.student.family

import androidx.lifecycle.SavedStateHandle
import com.stemtutor.core.data.audio.FakeAudioPlayer
import com.stemtutor.core.data.audio.FakeAudioRepository
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.family.FakeFamilyRepository
import com.stemtutor.core.data.learning.FakeLearningRepository
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.PlayableLesson
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.feature.student.lesson.LESSON_ARG
import com.stemtutor.feature.student.lesson.LessonPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A learning repository whose lessons wait for consent (DPDP, M4). */
private class NeedsConsent(private val real: LearningRepository = FakeLearningRepository()) : LearningRepository by real {
    override suspend fun startLesson(lessonId: String): Result<PlayableLesson> =
        Result.failure(AppException(AppError.Api(ApiErrorCode.CONSENT_REQUIRED)))
}

@OptIn(ExperimentalCoroutinesApi::class)
class LinkParentTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun showsACodeAndTheLinkedParents() {
        val vm = LinkParentViewModel(FakeFamilyRepository())
        assertEquals(FakeFamilyRepository.DEMO_CODE, vm.state.value.code!!.code)
        assertEquals(listOf("Meena"), vm.state.value.parents.map { it.name })
        vm.remove("p1")
        assertTrue(vm.state.value.parents.isEmpty())
    }

    @Test
    fun lessonsWaitForConsent() {
        val vm = LessonPlayerViewModel(
            NeedsConsent(),
            ListenController(FakeAudioRepository(), FakeAudioPlayer()),
            SavedStateHandle(mapOf(LESSON_ARG to "fake-lesson-1")),
        )
        assertEquals(AppError.Api(ApiErrorCode.CONSENT_REQUIRED), vm.state.value.loadError)
    }
}
