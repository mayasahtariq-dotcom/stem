package com.stemtutor.core.common.language

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LanguageRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val applied = mutableListOf<LanguageMode>()

    /** One "process": a fresh DataStore and repository over the same file. */
    private fun newProcess(file: File): Pair<LanguageRepository, CoroutineScope> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        return LanguageRepository(store) { applied += it } to scope
    }

    @Test
    fun defaultsToHindi() = runTest {
        val (repo, scope) = newProcess(File(tmp.root, "a.preferences_pb"))
        assertEquals(LanguageMode.HINDI, repo.current())
        scope.cancel()
    }

    @Test
    fun languageModeSurvivesRestart() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")

        val (first, firstScope) = newProcess(file)
        first.setLanguageMode(LanguageMode.KANNADA)
        // App process dies: wait until the first DataStore has fully released the file,
        // since DataStore refuses two active instances on one file.
        firstScope.coroutineContext.job.cancelAndJoin()

        val (second, secondScope) = newProcess(file)
        assertEquals(LanguageMode.KANNADA, second.current())
        second.applyStored()
        assertEquals(listOf(LanguageMode.KANNADA, LanguageMode.KANNADA), applied)
        secondScope.cancel()
    }

    @Test
    fun flowEmitsOnChange() = runTest {
        val (repo, scope) = newProcess(File(tmp.root, "b.preferences_pb"))
        repo.languageMode.test {
            assertEquals(LanguageMode.HINDI, awaitItem())
            repo.setLanguageMode(LanguageMode.KANNADA)
            assertEquals(LanguageMode.KANNADA, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        scope.cancel()
    }
}
