package com.stemtutor.app.prototype

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen

/** The page bundled in `src/debug/assets/mooshala/` (copy of docs/design/mooshala-code.md). */
const val PROTOTYPE_URL = "file:///android_asset/mooshala/index.html"

enum class PrototypeLoad { Loading, Ready, Failed }

/**
 * Debug builds only: the Mooshala web prototype in a WebView, as a design reference on a real phone.
 * It is separate from the native Compose screens. The prototype's fonts come from Google Fonts,
 * so offline it falls back to system fonts.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrototypeScreen(modifier: Modifier = Modifier) {
    var load by remember { mutableStateOf(PrototypeLoad.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    Box(modifier.fillMaxSize().statusBarsPadding()) {
        if (load != PrototypeLoad.Failed) {
            // `key` recreates the WebView on retry.
            androidx.compose.runtime.key(attempt) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Assets only: no access to the app's other files or the network pages.
                            settings.allowFileAccess = true
                            settings.allowContentAccess = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    load = PrototypeLoad.Loading
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    if (load != PrototypeLoad.Failed) load = PrototypeLoad.Ready
                                }

                                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                    // Fonts or other sub-resources failing is fine; only the page itself matters.
                                    if (request.isForMainFrame) load = PrototypeLoad.Failed
                                }

                                // Stay inside the bundled prototype.
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                                    !request.url.toString().startsWith("file:///android_asset/mooshala/")
                            }
                            loadUrl(PROTOTYPE_URL)
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
        when (load) {
            PrototypeLoad.Loading -> LoadingScreen()
            PrototypeLoad.Failed -> ErrorScreen(onRetry = {
                load = PrototypeLoad.Loading
                attempt++
            })
            PrototypeLoad.Ready -> Unit
        }
    }
}
