package com.stemtutor.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** The FastAPI backend. Every data read and write goes through here, never to tables directly. */
interface StemApi {
    @GET("health")
    suspend fun health(): HealthDto
}

@Serializable
data class HealthDto(val status: String, val version: String)
