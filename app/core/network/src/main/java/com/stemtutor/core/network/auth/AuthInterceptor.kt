package com.stemtutor.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Supplies the Supabase access token. Implemented by the auth repository. */
interface AccessTokenProvider {
    suspend fun accessToken(): String?

    /** Refreshes the session and returns the new token, or null if refreshing failed. */
    suspend fun refreshAccessToken(): String?
}

private const val AUTH_HEADER = "Authorization"

/** Attaches `Authorization: Bearer <token>` to every request when signed in. */
class AuthInterceptor(private val tokens: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokens.accessToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) header(AUTH_HEADER, "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}

/**
 * On a 401, refreshes the token once and retries. If the retry is also rejected, or the refresh
 * fails, gives up so the 401 reaches the caller (which then signs the user out).
 */
class TokenRefreshAuthenticator(private val tokens: AccessTokenProvider) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // already retried once
        val fresh = runBlocking { tokens.refreshAccessToken() } ?: return null
        return response.request.newBuilder().header(AUTH_HEADER, "Bearer $fresh").build()
    }
}
