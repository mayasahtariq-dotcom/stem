package com.stemtutor.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Short error codes returned by the backend as `{"detail": {"code": "..."}}`. */
enum class ApiErrorCode {
    UNAUTHORIZED,
    PROFILE_INCOMPLETE,
    NOT_A_TEACHER,
    NOT_A_STUDENT,
    NOT_FOUND,
    KEY_NOT_FOUND,
    KEY_EXPIRED,
    ALREADY_MEMBER,
    LANGUAGE_NOT_ALLOWED,
    TOO_MANY_ATTEMPTS,
    KEY_GENERATION_FAILED,
    INVALID_UPLOAD,
    FILE_TOO_LARGE,
    PAGES_MISSING,
    LESSON_NOT_PUBLISHED,
    LESSON_NOT_READY,
    INVALID_QUESTION,
    AI_UNAVAILABLE,
    PHONE_IN_USE,
    HEARTS_EMPTY,
    DOUBT_LIMIT,
    INVALID_ANSWER,
    LESSON_NOT_STARTED,
    CONSENT_REQUIRED,
    CODE_NOT_FOUND,
    CODE_EXPIRED,
    TOO_MANY_PARENTS,
    NOT_A_PARENT,
    ;

    companion object {
        fun fromCode(code: String?): ApiErrorCode? = entries.firstOrNull { it.name == code }
    }
}

/** Everything a repository call can fail with. The UI maps each case to a bilingual message. */
sealed interface AppError {
    data object Network : AppError
    data class Api(val code: ApiErrorCode) : AppError
    data object Unknown : AppError
}

class AppException(val error: AppError) : Exception(error.toString())

@Serializable
private data class ErrorDetail(val code: String? = null)

@Serializable
private data class ErrorBody(val detail: ErrorDetail? = null)

private val lenient = Json { ignoreUnknownKeys = true }

fun Throwable.toAppError(): AppError = when (this) {
    is AppException -> error
    is IOException -> AppError.Network
    is HttpException -> {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val code = body?.let { runCatching { lenient.decodeFromString<ErrorBody>(it) }.getOrNull() }
            ?.detail?.code
        ApiErrorCode.fromCode(code)?.let { AppError.Api(it) }
            ?: if (code() == 401) AppError.Api(ApiErrorCode.UNAUTHORIZED) else AppError.Unknown
    }
    else -> AppError.Unknown
}

/** Runs [block], turning any failure into an [AppException] carrying an [AppError]. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        Result.failure(AppException(e.toAppError()))
    }

val Throwable.appError: AppError get() = (this as? AppException)?.error ?: toAppError()
