package com.stemtutor.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.session.PendingJoinKey
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * AppCompatActivity so per-app locale changes recreate it with the new strings. Join links
 * (stemtutor://join/KEY and https://<host>/join/KEY) are handed to [PendingJoinKey], which
 * opens the join screen once the user is in the student section.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var pendingJoinKey: PendingJoinKey

    @Inject lateinit var watcher: GenerationWatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleLink(intent)
        setContent {
            StemTutorTheme { AppNavHost(pendingJoinKey, watcher) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) pendingJoinKey.offer(intent.dataString)
    }
}
