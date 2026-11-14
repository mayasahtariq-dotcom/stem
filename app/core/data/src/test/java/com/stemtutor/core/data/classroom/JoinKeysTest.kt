package com.stemtutor.core.data.classroom

import com.stemtutor.core.data.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinKeysTest {
    @Test
    fun sanitizeUppercasesAndDropsJunk() {
        assertEquals("BI09K4X2", JoinKeys.sanitize("bi09-k4x2"))
        assertEquals("BI09K4X2", JoinKeys.sanitize(" b i 0 9 k 4 x 2 9 9"))
        assertEquals("", JoinKeys.sanitize("--  "))
    }

    @Test
    fun formatInsertsDashAfterFourCharacters() {
        assertEquals("BI0", JoinKeys.format("BI0"))
        assertEquals("BI09", JoinKeys.format("BI09"))
        assertEquals("BI09-K", JoinKeys.format("BI09K"))
        assertEquals("BI09-K4X2", JoinKeys.format("BI09K4X2"))
    }

    @Test
    fun completeOnlyForValidKeys() {
        assertTrue(JoinKeys.isComplete("BI09K4X2"))
        assertFalse(JoinKeys.isComplete("BI09K4X0")) // zero isn't in the alphabet
        assertFalse(JoinKeys.isComplete("BI05K4X2")) // grade outside 6-12
        assertFalse(JoinKeys.isComplete("BI09K4X"))
    }

    @Test
    fun findsKeyInLinksAndScannedText() {
        assertEquals("BI09-K4X2", JoinKeys.findIn("https://mooshala.example/join/BI09-K4X2"))
        assertEquals("BI09-K4X2", JoinKeys.findIn("stemtutor://join/bi09k4x2"))
        assertNull(JoinKeys.findIn("https://example.com/nothing-here"))
    }

    @Test
    fun generatedKeysUseTheSafeAlphabet() {
        repeat(1000) {
            val key = JoinKeys.generate(Subject.PHYSICS, 8)
            assertTrue(key, key.startsWith("PH08-"))
            assertTrue(key, key.substring(5).none { it in "0O1IL" })
            assertTrue(key, JoinKeys.isComplete(JoinKeys.sanitize(key)))
        }
    }
}
