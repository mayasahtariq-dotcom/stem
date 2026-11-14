package com.stemtutor.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StemApiTest {
    private val server = MockWebServer()
    private lateinit var api: StemApi

    @Before
    fun setUp() {
        server.start()
        api = NetworkFactory.stemApi(
            server.url("/").toString(),
            NetworkFactory.okHttpClient(enableLogging = false),
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun parsesHealthAndIgnoresUnknownFields() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"ok","version":"0.1.0","extra":1}"""))
        assertEquals(HealthDto("ok", "0.1.0"), api.health())
        assertEquals("/health", server.takeRequest().path)
    }
}
