package com.stemtutor.core.data.health

import com.stemtutor.core.network.HealthDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class HealthRepositoryTest {
    @Test
    fun mapsHealthyResponse() = runTest {
        val api = TestStemApi(health = { HealthDto("ok", "0.1.0") })
        assertEquals(BackendStatus(true, "0.1.0"), RealHealthRepository(api).check().getOrThrow())
    }

    @Test
    fun networkErrorBecomesFailure() = runTest {
        val api = TestStemApi(health = { throw IOException("offline") })
        assertTrue(RealHealthRepository(api).check().isFailure)
    }
}
