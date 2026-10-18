package com.cineverse.core.player.analytics

import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlayerError
import com.cineverse.core.player.model.VideoQuality

/**
 * Playback events for analytics. Bind implementations into the Hilt set
 * (`@Binds @IntoSet`); every method has an empty default.
 */
interface PlaybackAnalyticsListener {
    /** First frame of content (not ads) started playing for [request]. */
    fun onPlaybackStarted(request: PlaybackRequest) {}

    fun onBufferingStarted(request: PlaybackRequest, positionMs: Long) {}

    fun onBufferingEnded(request: PlaybackRequest, positionMs: Long, bufferingDurationMs: Long) {}

    /** Rendition changed, whether chosen by adaptive selection or by the user. */
    fun onQualityChanged(request: PlaybackRequest, quality: VideoQuality) {}

    fun onPlaybackError(request: PlaybackRequest, error: PlayerError) {}

    /** Called every [PROGRESS_INTERVAL_MS] while content is playing. */
    fun onProgress(request: PlaybackRequest, positionMs: Long, durationMs: Long) {}

    companion object {
        const val PROGRESS_INTERVAL_MS = 15_000L
    }
}
