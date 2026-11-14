package com.stemtutor.core.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.model.Profile
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.model.fromApi
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.core.network.MeDto
import com.stemtutor.core.network.ProfileUpdateDto
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.apiCall
import kotlinx.coroutines.flow.first

interface ProfileRepository {
    suspend fun me(): Result<Profile>

    suspend fun save(name: String, languageMode: LanguageMode, roles: List<Role>): Result<Profile>
}

internal fun MeDto.toProfile() = Profile(
    id = id,
    registered = registered,
    name = name.orEmpty(),
    phone = phone.orEmpty(),
    roles = roles.mapNotNull(Role::fromApi),
    languageMode = LanguageMode.fromApi(languageMode),
)

class RemoteProfileRepository(private val api: StemApi) : ProfileRepository {
    override suspend fun me(): Result<Profile> = apiCall { api.me().toProfile() }

    override suspend fun save(name: String, languageMode: LanguageMode, roles: List<Role>) = apiCall {
        api.updateMe(ProfileUpdateDto(name.trim(), languageMode.storageKey, roles.map { it.apiValue }))
            .toProfile()
    }
}

/** Debug/test profile store, kept in DataStore so returning users skip onboarding. */
class FakeProfileRepository(
    private val auth: AuthRepository,
    private val dataStore: DataStore<Preferences>,
) : ProfileRepository {
    override suspend fun me(): Result<Profile> {
        val session = auth.currentSession()
            ?: return Result.failure(AppException(AppError.Api(ApiErrorCode.UNAUTHORIZED)))
        val prefs = dataStore.data.first()
        val name = prefs[key(session.userId, "name")]
            ?: return Result.success(Profile(id = session.userId, registered = false))
        return Result.success(
            Profile(
                id = session.userId,
                registered = true,
                name = name,
                phone = session.phone,
                roles = prefs[key(session.userId, "roles")].orEmpty().split(',').mapNotNull(Role::fromApi),
                languageMode = LanguageMode.fromApi(prefs[key(session.userId, "mode")]),
            ),
        )
    }

    override suspend fun save(name: String, languageMode: LanguageMode, roles: List<Role>): Result<Profile> {
        val session = auth.currentSession()
            ?: return Result.failure(AppException(AppError.Api(ApiErrorCode.UNAUTHORIZED)))
        dataStore.edit {
            it[key(session.userId, "name")] = name.trim()
            it[key(session.userId, "roles")] = roles.distinct().joinToString(",") { r -> r.apiValue }
            it[key(session.userId, "mode")] = languageMode.storageKey
        }
        return me()
    }

    private fun key(userId: String, field: String) = stringPreferencesKey("fake_profile_${userId}_$field")
}
