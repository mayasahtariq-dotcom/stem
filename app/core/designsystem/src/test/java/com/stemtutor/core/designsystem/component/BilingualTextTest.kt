package com.stemtutor.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.designsystem.R
import com.stemtutor.core.designsystem.text.ProvideLanguageMode
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Compose UI test, run on the JVM with Robolectric so CI's testDebugUnitTest covers it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BilingualTextTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsRegionalAndEnglishLines() {
        compose.setContent {
            StemTutorTheme {
                ProvideLanguageMode(LanguageMode.HINDI) {
                    BilingualText(R.string.ds_sample_option_mitochondria)
                }
            }
        }
        compose.onNodeWithTag(BilingualTextTags.REGIONAL)
            .assertIsDisplayed()
            .assertTextEquals("माइटोकॉन्ड्रिया")
        compose.onNodeWithTag(BilingualTextTags.ENGLISH)
            .assertIsDisplayed()
            .assertTextEquals("Mitochondria")
    }

    @Test
    fun switchingModeChangesRegionalLineOnly() {
        var mode by mutableStateOf(LanguageMode.HINDI)
        compose.setContent {
            StemTutorTheme {
                ProvideLanguageMode(mode) { BilingualText(R.string.ds_sample_check) }
            }
        }
        compose.onNodeWithTag(BilingualTextTags.REGIONAL).assertTextEquals("जाँचें")
        compose.onNodeWithTag(BilingualTextTags.ENGLISH).assertTextEquals("Check")

        mode = LanguageMode.KANNADA
        compose.waitForIdle()

        compose.onNodeWithTag(BilingualTextTags.REGIONAL).assertTextEquals("ಪರಿಶೀಲಿಸಿ")
        compose.onNodeWithTag(BilingualTextTags.ENGLISH).assertTextEquals("Check")
    }
}
