package com.stemtutor.core.designsystem.text

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.stemtutor.core.common.language.LanguageMode
import java.util.Locale

/** A string in the regional language (Hindi or Kannada) plus its English line. */
data class BilingualString(val regional: String, val english: String)

/**
 * Returns both lines for a string resource: the regional line from the active locale
 * (`values-hi` / `values-kn`) and the English line from the default `values` folder.
 */
@Composable
fun bilingualStringResource(@StringRes id: Int, vararg formatArgs: Any): BilingualString {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val englishResources = remember(context, configuration) {
        context.englishContext(configuration).resources
    }
    return BilingualString(
        regional = context.resources.getString(id, *formatArgs),
        english = englishResources.getString(id, *formatArgs),
    )
}

/** Non-Compose variant, for code that holds a Context. */
fun Context.bilingualString(@StringRes id: Int, vararg formatArgs: Any): BilingualString =
    BilingualString(
        regional = getString(id, *formatArgs),
        english = englishContext(resources.configuration).getString(id, *formatArgs),
    )

/** The language mode implied by the active configuration (Hindi unless the locale is Kannada). */
@Composable
@ReadOnlyComposable
fun currentLanguageMode(): LanguageMode = LocalConfiguration.current.languageMode()

fun Configuration.languageMode(): LanguageMode =
    if (locales[0]?.language == LanguageMode.KANNADA.localeTag) {
        LanguageMode.KANNADA
    } else {
        LanguageMode.HINDI
    }

private fun Context.englishContext(base: Configuration): Context {
    val english = Configuration(base).apply { setLocale(Locale.ENGLISH) }
    return createConfigurationContext(english)
}
