package com.stemtutor.feature.onboarding.otp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

private val SIX_DIGITS = Regex("(?<!\\d)(\\d{6})(?!\\d)")

/** Pulls the first 6-digit code out of an SMS body. */
fun extractOtp(message: String?): String? = message?.let { SIX_DIGITS.find(it)?.groupValues?.get(1) }

/**
 * Fills the OTP from the incoming SMS where possible, using the SMS User Consent API: Android
 * shows the message and asks the user once; no SMS permission is needed. Does nothing on
 * devices without Google Play services, where the user just types the code.
 */
@Composable
fun SmsCodeListener(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val latest = rememberUpdatedState(onCode)
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            extractOtp(result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE))?.let { latest.value(it) }
        }
    }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val status = intent.extras?.let {
                    IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_STATUS, Status::class.java)
                }
                if (status?.statusCode == CommonStatusCodes.SUCCESS) {
                    IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                        ?.let { runCatching { consent.launch(it) } }
                }
            }
        }
        val started = runCatching {
            SmsRetriever.getClient(context).startSmsUserConsent(null)
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                SmsRetriever.SEND_PERMISSION,
                null,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.isSuccess
        onDispose { if (started) runCatching { context.unregisterReceiver(receiver) } }
    }
}
