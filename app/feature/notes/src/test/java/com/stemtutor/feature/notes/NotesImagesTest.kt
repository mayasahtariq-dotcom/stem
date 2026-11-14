package com.stemtutor.feature.notes

import androidx.compose.ui.geometry.Offset
import com.stemtutor.feature.notes.crop.dragCorner
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesImagesTest {
    @Test
    fun photosAreScaledToAtMost1600OnTheLongSide() {
        assertEquals(1600 to 1200, NotesImages.scaledSize(4000, 3000))
        assertEquals(900 to 1600, NotesImages.scaledSize(2700, 4800))
        assertEquals(1000 to 800, NotesImages.scaledSize(1000, 800)) // never upscales
    }

    @Test
    fun sharpEdgesScoreHigherThanAFlatImage() {
        val w = 32
        val h = 32
        val flat = IntArray(w * h) { 128 }
        val stripes = IntArray(w * h) { i -> if ((i % w) % 2 == 0) 0 else 255 }
        assertEquals(0.0, NotesImages.laplacianVariance(flat, w, h), 1e-9)
        assertTrue(NotesImages.laplacianVariance(stripes, w, h) > NotesImages.BLUR_THRESHOLD)
    }

    @Test
    fun cropFractionsBecomePixels() {
        assertArrayEquals(intArrayOf(0, 0, 100, 50), NotesImages.cropPixels(CropRect.FULL, 100, 50))
        assertArrayEquals(intArrayOf(10, 5, 80, 40), NotesImages.cropPixels(CropRect(0.1f, 0.1f, 0.9f, 0.9f), 100, 50))
    }

    @Test
    fun draggingMovesTheNearestCornerAndKeepsAMinimumSize() {
        val moved = dragCorner(CropRect.FULL, Offset(0.2f, 0.3f))
        assertEquals(CropRect(0.2f, 0.3f, 1f, 1f), moved)
        val squeezed = dragCorner(CropRect(0.5f, 0.5f, 0.6f, 0.6f), Offset(0.7f, 0.7f))
        assertTrue(squeezed.right - squeezed.left >= CropRect.MIN_SIZE - 1e-6f)
    }
}
