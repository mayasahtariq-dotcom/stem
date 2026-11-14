package com.stemtutor.core.designsystem.text

import androidx.annotation.StringRes
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.ApiErrorCode

/** Every error the app can show, as a bilingual string resource. Never a blank screen. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.err_network
    AppError.Unknown -> R.string.err_unknown
    is AppError.Api -> when (code) {
        ApiErrorCode.UNAUTHORIZED -> R.string.err_unauthorized
        ApiErrorCode.PROFILE_INCOMPLETE -> R.string.err_profile_incomplete
        ApiErrorCode.NOT_A_TEACHER -> R.string.err_not_a_teacher
        ApiErrorCode.NOT_A_STUDENT -> R.string.err_not_a_student
        ApiErrorCode.NOT_FOUND -> R.string.err_not_found
        ApiErrorCode.KEY_NOT_FOUND -> R.string.err_key_not_found
        ApiErrorCode.KEY_EXPIRED -> R.string.err_key_expired
        ApiErrorCode.ALREADY_MEMBER -> R.string.err_already_member
        ApiErrorCode.LANGUAGE_NOT_ALLOWED -> R.string.err_language_not_allowed
        ApiErrorCode.TOO_MANY_ATTEMPTS -> R.string.err_too_many_attempts
        ApiErrorCode.KEY_GENERATION_FAILED -> R.string.err_unknown
    }
}
