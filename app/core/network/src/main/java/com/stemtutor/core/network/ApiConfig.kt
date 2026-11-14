package com.stemtutor.core.network

/**
 * Backend settings, provided by the app module from BuildConfig (filled from local.properties).
 * When [useRealApi] is false, repositories use their fake implementations.
 */
data class ApiConfig(
    val baseUrl: String,
    val useRealApi: Boolean,
    val enableHttpLogging: Boolean,
)
