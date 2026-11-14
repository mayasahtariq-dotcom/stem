package com.stemtutor.core.network.auth

import com.stemtutor.core.network.NetworkFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class AuthInterceptorTest {
    private val server = MockWebServer()

    private class Tokens(var current: String?, private val refreshed: String?) : AccessTokenProvider {
        var refreshes = 0
        override suspend fun accessToken() = current
        override suspend fun refreshAccessToken(): String? {
            refreshes++
            current = refreshed
            return refreshed
        }
    }

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    private fun api(tokens: AccessTokenProvider) =
        NetworkFactory.stemApi(server.url("/").toString(), NetworkFactory.okHttpClient(false, tokens))

    @Test
    fun attachesBearerToken() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"ok","version":"1"}"""))
        api(Tokens("abc", null)).health()
        assertEquals("Bearer abc", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun noHeaderWhenSignedOut() = runTest {
        server.enqueue(MockResponse().setBody("""{"status":"ok","version":"1"}"""))
        api(Tokens(null, null)).health()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun refreshesOnceOn401ThenRetries() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("""{"status":"ok","version":"1"}"""))
        val tokens = Tokens("old", "new")
        api(tokens).health()
        assertEquals("Bearer old", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer new", server.takeRequest().getHeader("Authorization"))
        assertEquals(1, tokens.refreshes)
    }

    @Test
    fun givesUpAfterOneRefresh() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        val tokens = Tokens("old", "new")
        val error = runCatching { api(tokens).health() }.exceptionOrNull()
        assertEquals(401, (error as HttpException).code())
        assertEquals(1, tokens.refreshes)
        assertEquals(2, server.requestCount)
    }
}
