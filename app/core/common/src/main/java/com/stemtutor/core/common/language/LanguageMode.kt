package com.stemtutor.core.common.language

/**
 * The user's language mode: the regional language shown first, always with English underneath.
 * [localeTag] is the Android resource locale used for the regional strings.
 */
enum class LanguageMode(val localeTag: String, val storageKey: String) {
    HINDI("hi", "hindi"),
    KANNADA("kn", "kannada"),
    ;

    companion object {
        val DEFAULT = HINDI

        fun fromStorageKey(key: String?): LanguageMode =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}
