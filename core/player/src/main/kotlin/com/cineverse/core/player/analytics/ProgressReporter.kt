package com.cineverse.core.player.analytics

import com.cineverse.core.player.model.PlaybackRequest

/**
 * Sends watch progress to the backend (continue watching, entitlement checks). Bind
 * implementations into the Hilt set (`@Binds @IntoSet`).
 */
interface ProgressReporter {
    /**
     * Called every 15 seconds while playing, and with [isFinal] = true when playback pauses,
     * ends or is released. Not called for live streams.
     */
    fun reportProgress(request: PlaybackRequest, positionMs: Long, durationMs: Long, isFinal: Boolean)
}
