package com.cineverse.core.player

import android.view.ViewGroup
import androidx.media3.common.Player
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

/**
 * The player API that features build on. It holds no UI code, so a phone UI and a TV UI can
 * both drive it unchanged. Create one per screen with [PlayerControllerFactory] and call
 * [release] when the screen goes away.
 */
interface PlayerController {

    val state: StateFlow<PlayerState>

    /**
     * Player to attach to a video surface. While casting it forwards to the Cast device, so the
     * surface shows nothing and the UI should show a "Casting" placeholder instead.
     */
    val player: Player

    /**
     * Container for the IMA ad UI. UI modules must host it on screen during ad playback. It is
     * owned by the controller so it survives configuration changes.
     */
    val adViewContainer: ViewGroup

    /** True when Google Cast is available on this device, so a Cast button can be shown. */
    val isCastAvailable: Boolean

    /** Loads [request] and starts playback. Calling it again with the same id does nothing. */
    fun load(request: PlaybackRequest)

    fun play()

    fun pause()

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun seekForward(incrementMs: Long = DEFAULT_SEEK_INCREMENT_MS)

    fun seekBack(incrementMs: Long = DEFAULT_SEEK_INCREMENT_MS)

    /** Locks video to one quality from [PlayerState.videoQualities]; null restores adaptive. */
    fun setQuality(qualityId: String?)

    fun setAudioTrack(trackId: String)

    /** Selects a subtitle track from [PlayerState.subtitleTracks]; null turns subtitles off. */
    fun setSubtitleTrack(trackId: String?)

    /** Ignored for live streams. */
    fun setSpeed(speed: Float)

    /** Jumps to the live edge of a live stream. */
    fun goToLive()

    /** Clears an error and prepares the current item again. */
    fun retry()

    /** Stops playback and frees the decoder, ads loader, MediaSession and Cast resources. */
    fun release()

    companion object {
        const val DEFAULT_SEEK_INCREMENT_MS = 10_000L
    }
}
