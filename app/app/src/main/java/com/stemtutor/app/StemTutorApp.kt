package com.stemtutor.app

import android.app.Application
import com.stemtutor.core.common.language.LanguageRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class StemTutorApp : Application() {
    @Inject lateinit var languageRepository: LanguageRepository

    override fun onCreate() {
        super.onCreate()
        // Apply the stored language mode before any activity starts, so the UI follows the
        // user's choice (default Hindi), never the phone's system language.
        runBlocking { languageRepository.applyStored() }
    }
}
