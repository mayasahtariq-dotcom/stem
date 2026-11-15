package com.stemtutor.feature.student.lesson

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.learning.FakeLearningRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonLogicTest {
    @Test
    fun wrongAnswersAreAskedOnceMoreAtTheEnd() {
        var q = LessonQueue.of(3)
        q = q.answered(correct = false).next() // question 0 wrong -> retried at the end
        q = q.answered(correct = true).next()
        q = q.answered(correct = true).next()
        assertEquals(Turn(0, retry = true), q.current)
        q = q.answered(correct = false).next() // a second miss isn't added again
        assertTrue(q.finished)
        assertEquals(4, q.turns.size)
    }

    @Test
    fun progressCountsAnsweredTurns() {
        val q = LessonQueue.of(4).next()
        assertEquals(0.25f, q.progress, 0.001f)
    }

    @Test
    fun matchPairsMatchesFadesAndCountsMistakes() {
        var m = MatchState(pairCount = 3, rightOrder = listOf(2, 0, 1))
        m = m.tapRight(0) // nothing selected on the left: ignored
        assertEquals(0, m.mistakes)
        m = m.tapLeft(0).tapRight(0) // slot 0 holds pair 2: wrong, shakes
        assertEquals(1, m.mistakes)
        assertEquals(0, m.wrongSlot)
        assertNull(m.selectedLeft)
        m = m.tapLeft(0).tapRight(1) // slot 1 holds pair 0: matched
        assertEquals(setOf(0), m.matched)
        assertNull(m.wrongSlot)
        m = m.tapLeft(1).tapRight(2).tapLeft(2).tapRight(0)
        assertTrue(m.done)
        assertEquals(1, m.mistakes)
    }

    @Test
    fun shuffledRightColumnIsStableAndNotInOrder() {
        val a = MatchState.of(4, seed = 7)
        val b = MatchState.of(4, seed = 7)
        assertEquals(a.rightOrder, b.rightOrder)
        assertFalse(a.rightOrder == listOf(0, 1, 2, 3))
        assertEquals(setOf(0, 1, 2, 3), a.rightOrder.toSet())
    }

    @Test
    fun termWordsAreFoundInBothLines() {
        val terms = FakeLearningRepository.TERMS
        val hi = "माइटोकॉन्ड्रिया भोजन से ऊर्जा बनाता है"
        val spans = TermMatcher.find(hi, terms, LanguageMode.HINDI, english = false)
        assertEquals(listOf(TermSpan(0, "माइटोकॉन्ड्रिया".length, "mitochondria")), spans)
        val en = TermMatcher.find("The cell wall and the Mitochondria.", terms, LanguageMode.HINDI, english = true)
        assertEquals(listOf("cell_wall", "mitochondria"), en.map { it.key })
        // Whole words only in English: "nucleus" inside "nucleuses" doesn't count as a match here.
        assertTrue(TermMatcher.find("nucleuses", terms, LanguageMode.HINDI, english = true).isEmpty())
    }

    @Test
    fun glossarySearchWorksInAnyLanguage() {
        val mito = FakeLearningRepository.TERMS.first { it.key == "mitochondria" }
        assertTrue(mito.matches("mito"))
        assertTrue(mito.matches("माइटो"))
        assertTrue(mito.matches("ಮೈಟೊ"))
        assertTrue(mito.matches("सूत्रकणिका")) // formal name
        assertTrue(mito.matches(""))
        assertFalse(mito.matches("ribo"))
    }

    @Test
    fun minutesUntilReadsIsoAndEpoch() {
        assertEquals(30, minutesUntil("${1_800_000L}", now = 0L))
        assertEquals(60, minutesUntil("2026-11-15T07:00:00Z", now = java.time.Instant.parse("2026-11-15T06:00:00Z").toEpochMilli()))
        assertNull(minutesUntil("soon"))
    }
}
