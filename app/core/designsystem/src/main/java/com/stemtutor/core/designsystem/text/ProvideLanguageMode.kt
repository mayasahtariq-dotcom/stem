package com.stemtutor.core.designsystem.text

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.stemtutor.core.common.language.LanguageMode
import java.util.Locale

/**
 * Renders [content] as if the per-app locale were [mode]. Used by tests and previews; the
 * running app instead applies the mode with AppCompatDelegate.setApplicationLocales.
 */
@Composable
fun ProvideLanguageMode(mode: LanguageMode, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val configuration = remember(mode, baseConfiguration) {
        Configuration(baseConfiguration).apply { setLocale(Locale.forLanguageTag(mode.localeTag)) }
    }
    val context = remember(configuration, baseContext) {
        baseContext.createConfigurationContext(configuration)
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides context,
        content = content,
    )
}
