package com.stemtutor.core.data.auth

import com.stemtutor.core.network.auth.AccessTokenProvider
import kotlinx.coroutines.flow.Flow

/** A signed-in user, as far as login is concerned. */
data class AuthSession(val userId: String, val phone: String)

/**
 * Phone OTP login. The real version wraps Supabase (supabase-kt, used only for login); the fake
 * accepts any number with code 123456. Every data call goes to our backend with the token.
 */
interface AuthRepository : AccessTokenProvider {
    val session: Flow<AuthSession?>

    suspend fun currentSession(): AuthSession?

    /** [phone] is the 10-digit Indian mobile number; +91 is added here. */
    suspend fun sendOtp(phone: String): Result<Unit>

    suspend fun verifyOtp(phone: String, code: String): Result<AuthSession>

    suspend fun signOut()
}

fun indianE164(tenDigits: String): String = "+91$tenDigits"
