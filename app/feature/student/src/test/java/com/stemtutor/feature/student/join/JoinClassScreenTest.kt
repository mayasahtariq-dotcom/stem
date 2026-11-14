package com.stemtutor.feature.student.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.designsystem.text.ProvideLanguageMode
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.core.network.AppError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The join screen shows the right bilingual message for each error code. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class JoinClassScreenTest {
    @get:Rule val compose = createComposeRule()

    private val expected = mapOf(
        ApiErrorCode.KEY_NOT_FOUND to Triple(
            "इस की से कोई क्लास नहीं मिली। अपने टीचर से की जाँचें।",
            "ಈ ಕೀ ಇರುವ ತರಗತಿ ಇಲ್ಲ. ನಿಮ್ಮ ಶಿಕ್ಷಕರ ಬಳಿ ಕೀ ಪರಿಶೀಲಿಸಿ.",
            "No class has this key. Check it with your teacher.",
        ),
        ApiErrorCode.KEY_EXPIRED to Triple(
            "यह क्लास की पुरानी हो गई है। टीचर से नई की माँगें।",
            "ಈ ತರಗತಿ ಕೀ ಅವಧಿ ಮುಗಿದಿದೆ. ಶಿಕ್ಷಕರಿಂದ ಹೊಸ ಕೀ ಕೇಳಿ.",
            "This class key has expired. Ask your teacher for a new one.",
        ),
        ApiErrorCode.ALREADY_MEMBER to Triple(
            "तुम पहले से इस क्लास में हो।",
            "ನೀವು ಈಗಾಗಲೇ ಈ ತರಗತಿಯಲ್ಲಿದ್ದೀರಿ.",
            "You're already in this class.",
        ),
        ApiErrorCode.LANGUAGE_NOT_ALLOWED to Triple(
            "यह क्लास तुम्हारी भाषा में नहीं है। टीचर से पूछें या प्रोफ़ाइल में भाषा बदलें।",
            "ಈ ತರಗತಿ ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿಲ್ಲ. ಶಿಕ್ಷಕರನ್ನು ಕೇಳಿ ಅಥವಾ ಪ್ರೊಫೈಲ್‌ನಲ್ಲಿ ಭಾಷೆ ಬದಲಿಸಿ.",
            "This class doesn't use your language. Ask your teacher, or change your language in Profile.",
        ),
        ApiErrorCode.TOO_MANY_ATTEMPTS to Triple(
            "बहुत बार कोशिश हो गई। एक घंटे बाद फिर कोशिश करें।",
            "ತುಂಬಾ ಬಾರಿ ಪ್ರಯತ್ನಿಸಿದ್ದೀರಿ. ಒಂದು ಗಂಟೆ ನಂತರ ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ.",
            "Too many tries. Please wait an hour and try again.",
        ),
    )

    @Test
    fun eachErrorCodeShowsItsBilingualMessage() {
        var mode by mutableStateOf(LanguageMode.HINDI)
        var code by mutableStateOf(ApiErrorCode.KEY_NOT_FOUND)
        compose.setContent {
            StemTutorTheme {
                ProvideLanguageMode(mode) {
                    JoinClassScreen(
                        state = JoinClassUiState(key = "BI09K4X2", error = AppError.Api(code)),
                        onKeyChange = {}, onScan = {}, onJoin = {}, onDone = {}, onBack = {},
                        animate = false,
                    )
                }
            }
        }
        for ((errorCode, texts) in expected) {
            for (m in LanguageMode.entries) {
                mode = m
                code = errorCode
                compose.waitForIdle()
                val regional = if (m == LanguageMode.HINDI) texts.first else texts.second
                compose.onNodeWithTag("join_error", useUnmergedTree = true).assertExists()
                compose.onNode(hasText(regional), useUnmergedTree = true).assertExists()
                compose.onNode(hasText(texts.third), useUnmergedTree = true).assertExists()
            }
        }
    }

    @Test
    fun joinIsDisabledUntilAPreviewLoads() {
        compose.setContent {
            StemTutorTheme {
                JoinClassScreen(JoinClassUiState(key = "BI09"), {}, {}, {}, {}, {}, animate = false)
            }
        }
        compose.onNodeWithTag("join_button").assertIsNotEnabled()
    }

    @Test
    fun keyBoxesUppercaseAndDropInvalidCharacters() {
        var key by mutableStateOf("")
        compose.setContent {
            StemTutorTheme { JoinKeyInput(value = key, onValueChange = { key = it }, modifier = Modifier.testTag("key")) }
        }
        compose.onNodeWithTag("key").performTextInput("bi09-k4x2!")
        assertEquals("BI09K4X2", key)
    }
}

