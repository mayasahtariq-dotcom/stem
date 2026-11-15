package com.stemtutor.app.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.stemtutor.app.BuildConfig
import com.stemtutor.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Debug builds can switch, at runtime, between the built-in fake backend and a real backend
 * (e.g. scripts/dev_server.py on a PC on the same Wi-Fi). Saved in DataStore; read once at
 * start, so changing it restarts the app. Release builds always use BuildConfig.
 */
object DevServerSettings {
    val USE_REAL = booleanPreferencesKey("dev_server_use_real")
    val BASE_URL = stringPreferencesKey("dev_server_base_url")

    /** Normalises what the user typed into a base URL Retrofit accepts, or null if invalid. */
    fun normalize(raw: String): String? {
        var url = raw.trim()
        if (url.isEmpty()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url += "/"
        val host = url.substringAfter("://").substringBefore("/").substringBefore(":")
        return url.takeIf { host.isNotBlank() && " " !in url }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideApiConfig(dataStore: DataStore<Preferences>): ApiConfig {
        var useReal = BuildConfig.USE_REAL_API
        var baseUrl = BuildConfig.API_BASE_URL
        if (BuildConfig.DEBUG) {
            val prefs = runBlocking { dataStore.data.first() }
            prefs[DevServerSettings.USE_REAL]?.let { useReal = it }
            prefs[DevServerSettings.BASE_URL]?.let { baseUrl = it }
        }
        // Real login needs the Supabase project settings; without them stay on the fake backend.
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) useReal = false
        return ApiConfig(
            baseUrl = baseUrl,
            useRealApi = useReal,
            enableHttpLogging = BuildConfig.DEBUG,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            joinLinkHost = BuildConfig.JOIN_LINK_HOST,
        )
    }
}
