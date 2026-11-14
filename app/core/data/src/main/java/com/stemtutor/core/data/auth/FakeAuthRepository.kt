package com.stemtutor.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Debug/test login: any 10-digit number, code [FAKE_OTP]. The "session" is kept in DataStore so
 * returning users skip login, like the real app.
 */
class FakeAuthRepository(private val dataStore: DataStore<Preferences>) : AuthRepository {
    override val session: Flow<AuthSession?> = dataStore.data.map { prefs ->
        prefs[PHONE]?.let { AuthSession(userId = fakeUserId(it), phone = it) }
    }

    override suspend fun currentSession(): AuthSession? = session.first()

    override suspend fun sendOtp(phone: String): Result<Unit> =
        if (phone.length == 10 && phone.all(Char::isDigit)) {
            Result.success(Unit)
        } else {
            Result.failure(AppException(AppError.Unknown))
        }

    override suspend fun verifyOtp(phone: String, code: String): Result<AuthSession> {
        if (code != FAKE_OTP) {
            return Result.failure(AppException(AppError.Api(ApiErrorCode.UNAUTHORIZED)))
        }
        val e164 = indianE164(phone)
        dataStore.edit { it[PHONE] = e164 }
        return Result.success(AuthSession(fakeUserId(e164), e164))
    }

    override suspend fun signOut() {
        dataStore.edit { it.remove(PHONE) }
    }

    override suspend fun accessToken(): String? = currentSession()?.let { "fake-token-${it.userId}" }

    override suspend fun refreshAccessToken(): String? = accessToken()

    companion object {
        const val FAKE_OTP = "123456"
        private val PHONE = stringPreferencesKey("fake_auth_phone")
        fun fakeUserId(phone: String) = "fake-${phone.filter(Char::isDigit)}"
    }
}
