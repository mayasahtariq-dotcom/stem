package com.stemtutor.feature.teacher.classroom

import androidx.lifecycle.SavedStateHandle
import com.stemtutor.core.data.family.FakeDashboardRepository
import com.stemtutor.core.designsystem.theme.StemColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClassDashboardTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun weakTopicBarsAreRedAmberGreen() {
        assertEquals(StemColors.Red, accuracyColor(0.49f))
        assertEquals(StemColors.Green, accuracyColor(0.76f))
        val amber = accuracyColor(0.5f)
        assertEquals(amber, accuracyColor(0.75f))
        assertTrue(amber != StemColors.Red && amber != StemColors.Green)
        assertEquals("72%", percent(0.72f))
        assertEquals("–", percent(null))
    }

    @Test
    fun studentsBehindComeFirstAndIdleOnesAreMarked() {
        val vm = ClassDashboardViewModel(FakeDashboardRepository(), SavedStateHandle(mapOf(CLASSROOM_ID_ARG to "c1")))
        val d = vm.state.value.dashboard!!
        val behind = d.students.takeWhile { it.behind }
        assertEquals(d.tiles.behind, behind.size)
        assertEquals(behind.map { it.name }.sorted(), behind.map { it.name })
        assertTrue(d.students.drop(behind.size).none { it.behind })
        assertTrue(d.students.filter { it.idle }.all { it.idleDays > 3 })
    }

    @Test
    fun schoolConsentToggles() {
        val vm = ClassDashboardViewModel(FakeDashboardRepository(), SavedStateHandle(mapOf(CLASSROOM_ID_ARG to "c1")))
        assertFalse(vm.state.value.dashboard!!.schoolConsent)
        vm.setSchoolConsent(true)
        assertTrue(vm.state.value.dashboard!!.schoolConsent)
    }
}
