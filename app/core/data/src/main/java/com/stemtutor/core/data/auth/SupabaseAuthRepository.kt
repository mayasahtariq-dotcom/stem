package com.stemtutor.core.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import com.stemtutor.core.network.apiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Real phone OTP login through Supabase Auth. supabase-kt is used for nothing else. */
class SupabaseAuthRepository(
    url: String,
    anonKey: String,
    sessionManager: SessionManager,
) : AuthRepository {
    private val client: SupabaseClient = createSupabaseClient(url, anonKey) {
        install(Auth) {
            this.sessionManager = sessionManager
            autoLoadFromStorage = true
            alwaysAutoRefresh = true
        }
    }

    override val session: Flow<AuthSession?> = client.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.let { user ->
            AuthSession(userId = user.id, phone = user.phone.orEmpty())
        }
    }

    override suspend fun currentSession(): AuthSession? {
        client.auth.awaitInitialization()
        return session.first()
    }

    override suspend fun sendOtp(phone: String): Result<Unit> = apiCall {
        client.auth.signInWith(OTP) { this.phone = indianE164(phone) }
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<AuthSession> = apiCall {
        client.auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = indianE164(phone), token = code)
        val user = requireNotNull(client.auth.currentUserOrNull()) { "no user after verifying OTP" }
        AuthSession(userId = user.id, phone = user.phone.orEmpty())
    }

    override suspend fun signOut() {
        runCatching { client.auth.signOut() }
        client.auth.clearSession()
    }

    override suspend fun accessToken(): String? = client.auth.currentAccessTokenOrNull()

    override suspend fun refreshAccessToken(): String? =
        runCatching { client.auth.refreshCurrentSession() }
            .map { client.auth.currentAccessTokenOrNull() }
            .getOrNull()
}
