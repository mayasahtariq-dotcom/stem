package com.stemtutor.core.common.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Uses AndroidX per-app locales; the current activity is recreated with the new strings. */
class AppCompatLocaleApplier : LocaleApplier {
    override fun apply(mode: LanguageMode) {
        val target = LocaleListCompat.forLanguageTags(mode.localeTag)
        if (AppCompatDelegate.getApplicationLocales() != target) {
            AppCompatDelegate.setApplicationLocales(target)
        }
    }
}
