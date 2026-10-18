package com.cineverse.core.player.analytics

import com.cineverse.core.player.model.PlaybackRequest

/**
 * Sends watch progress to the backend (continue watching, entitlement checks). Bind
 * implementations into the Hilt set (`@Binds @IntoSet`).
 */
interface ProgressReporter {
    /**
     * Called every 15 seconds while playing, and with [isFinal] = true when playback pauses,
     * ends or the player screen closes. Not called for live streams or during ads. The video
     * player feature's ViewModel calls it; :core:player only declares the hook.
     */
    fun reportProgress(request: PlaybackRequest, positionMs: Long, durationMs: Long, isFinal: Boolean)
}
