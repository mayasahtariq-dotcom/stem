package com.stemtutor.core.common.language

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Applies a language mode to the app's UI (per-app locale). Swappable in tests. */
fun interface LocaleApplier {
    fun apply(mode: LanguageMode)
}

/**
 * Source of truth for the language mode. Stored in DataStore (survives restarts) and applied as
 * the per-app locale, so string resources follow the user's choice, not the phone's language.
 */
class LanguageRepository(
    private val dataStore: DataStore<Preferences>,
    private val localeApplier: LocaleApplier,
) {
    val languageMode: Flow<LanguageMode> =
        dataStore.data.map { prefs -> LanguageMode.fromStorageKey(prefs[KEY]) }

    suspend fun current(): LanguageMode = languageMode.first()

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { it[KEY] = mode.storageKey }
        localeApplier.apply(mode)
    }

    /** Re-applies the stored mode; called once at app start. */
    suspend fun applyStored() {
        localeApplier.apply(current())
    }

    private companion object {
        val KEY = stringPreferencesKey("language_mode")
    }
}
