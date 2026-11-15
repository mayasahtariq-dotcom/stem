package com.stemtutor.core.data.audio

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.model.SpeechLanguage
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.network.AudioInDto
import com.stemtutor.core.network.LearningApi
import com.stemtutor.core.network.apiCall
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One clip to play. [cacheKey] stays the same for the same text, so the phone's cache works
 * even though download links change. */
data class Clip(val url: String, val cacheKey: String)

/** Plays clips one after another (regional line, then English). */
interface AudioPlayer {
    fun play(clips: List<Clip>)

    fun stop()
}

/** Gives a link to the audio for a text in a language (the server makes and caches it). */
interface AudioRepository {
    suspend fun url(text: String, language: SpeechLanguage): Result<String>
}

class RemoteAudioRepository(private val api: LearningApi) : AudioRepository {
    private val lock = Mutex()
    private val known = mutableMapOf<String, String>()

    override suspend fun url(text: String, language: SpeechLanguage): Result<String> {
        val key = "${language.code}|$text"
        lock.withLock { known[key] }?.let { return Result.success(it) }
        return apiCall { api.audio(AudioInDto(text, language.code)).url }
            .onSuccess { url -> lock.withLock { known[key] = url } }
    }
}

/** Debug builds and tests: no audio, but the calls are recorded. */
class FakeAudioRepository : AudioRepository {
    val requested = mutableListOf<Pair<String, SpeechLanguage>>()

    override suspend fun url(text: String, language: SpeechLanguage): Result<String> {
        requested += text to language
        return Result.success("fake://audio/${language.code}/${text.hashCode()}")
    }
}

class FakeAudioPlayer : AudioPlayer {
    val played = mutableListOf<List<Clip>>()

    override fun play(clips: List<Clip>) {
        played += clips
    }

    override fun stop() = Unit
}

/**
 * The Listen button (PRD M3): a tap plays the regional line, then the English line; a long
 * press plays only the English.
 */
class ListenController(private val audio: AudioRepository, private val player: AudioPlayer) {
    suspend fun speak(text: Tri, mode: LanguageMode, englishOnly: Boolean = false): Result<Unit> {
        val regional = if (mode == LanguageMode.KANNADA) SpeechLanguage.KANNADA else SpeechLanguage.HINDI
        val parts = buildList {
            if (!englishOnly && text.regional(mode).isNotBlank()) add(text.regional(mode) to regional)
            if (text.en.isNotBlank()) add(text.en to SpeechLanguage.ENGLISH)
        }
        val clips = mutableListOf<Clip>()
        for ((line, language) in parts) {
            val url = audio.url(line, language).getOrElse { return Result.failure(it) }
            clips += Clip(url, "${language.code}|$line")
        }
        if (clips.isNotEmpty()) player.play(clips)
        return Result.success(Unit)
    }

    fun stop() = player.stop()
}
