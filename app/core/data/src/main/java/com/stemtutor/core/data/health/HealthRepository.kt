package com.stemtutor.core.data.health

import com.stemtutor.core.network.StemApi

data class BackendStatus(val ok: Boolean, val version: String)

/** Reports whether the backend is reachable. Real and fake versions, chosen by ApiConfig. */
interface HealthRepository {
    suspend fun check(): Result<BackendStatus>
}

class RealHealthRepository(private val api: StemApi) : HealthRepository {
    override suspend fun check(): Result<BackendStatus> = runCatching {
        val dto = api.health()
        BackendStatus(ok = dto.status == "ok", version = dto.version)
    }
}

class FakeHealthRepository(
    private val result: Result<BackendStatus> = Result.success(BackendStatus(true, "fake")),
) : HealthRepository {
    override suspend fun check(): Result<BackendStatus> = result
}
