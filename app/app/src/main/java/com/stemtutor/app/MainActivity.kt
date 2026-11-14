package com.stemtutor.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import dagger.hilt.android.AndroidEntryPoint

/** AppCompatActivity so per-app locale changes recreate it with the new strings. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StemTutorTheme {
                // Debug builds open the component gallery; release builds a placeholder (M0).
                AppEntry()
            }
        }
    }
}
