package com.stemtutor.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stemtutor.core.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Small on-device settings: the role the user last used, so they return straight to it. */
class SessionStore(private val dataStore: DataStore<Preferences>) {
    val lastRole: Flow<Role?> = dataStore.data.map { prefs -> prefs[LAST_ROLE]?.let(Role::fromApi) }

    suspend fun lastRole(): Role? = lastRole.first()

    suspend fun setLastRole(role: Role) {
        dataStore.edit { it[LAST_ROLE] = role.apiValue }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(LAST_ROLE) }
    }

    private companion object {
        val LAST_ROLE = stringPreferencesKey("last_role")
    }
}
