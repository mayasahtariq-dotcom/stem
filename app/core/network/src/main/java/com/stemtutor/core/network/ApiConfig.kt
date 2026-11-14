package com.stemtutor.core.network

/**
 * Backend settings, provided by the app module from BuildConfig (filled from local.properties).
 * When [useRealApi] is false, repositories use their fake implementations (and a fake login).
 */
data class ApiConfig(
    val baseUrl: String,
    val useRealApi: Boolean,
    val enableHttpLogging: Boolean,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    /** Host for web join links, https://<host>/join/<KEY>. A placeholder until a domain exists. */
    val joinLinkHost: String = "mooshala.example",
)
