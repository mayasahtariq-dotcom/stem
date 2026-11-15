package com.stemtutor.core.network

import com.stemtutor.core.network.auth.AccessTokenProvider
import com.stemtutor.core.network.auth.AuthInterceptor
import com.stemtutor.core.network.auth.TokenRefreshAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun okHttpClient(enableLogging: Boolean, tokens: AccessTokenProvider? = null): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (tokens != null) {
                    addInterceptor(AuthInterceptor(tokens))
                    authenticator(TokenRefreshAuthenticator(tokens))
                }
                if (enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()

    fun stemApi(baseUrl: String, client: OkHttpClient): StemApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(StemApi::class.java)

    /** Doubt answers come from a text model and can take a while, so reads wait longer. */
    fun learningApi(baseUrl: String, client: OkHttpClient): LearningApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client.newBuilder().readTimeout(120, TimeUnit.SECONDS).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LearningApi::class.java)
}
