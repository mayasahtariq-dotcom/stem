package com.stemtutor.core.data.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

/** ExoPlayer with an on-phone cache of up to 50 MB (PRD M3). Call from the main thread. */
@OptIn(UnstableApi::class)
class ExoAudioPlayer(private val context: Context) : AudioPlayer {
    private val cache by lazy {
        SimpleCache(
            File(context.cacheDir, "lesson-audio"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context),
        )
    }

    private val player: ExoPlayer by lazy {
        val dataSource = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSource))
            .build()
    }

    override fun play(clips: List<Clip>) {
        val items = clips.filter { it.url.startsWith("http") }
            .map { MediaItem.Builder().setUri(it.url).setCustomCacheKey(it.cacheKey).build() }
        if (items.isEmpty()) return
        player.stop()
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }

    override fun stop() {
        player.stop()
    }

    private companion object {
        const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    }
}
