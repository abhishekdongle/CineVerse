package com.cineverse.feature.videoplayer.presentation

import android.view.ViewGroup
import androidx.media3.common.Player
import com.cineverse.core.player.PlayerController
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Test double for [PlayerController]: records every command the ViewModel sends in [commands]
 * and lets tests drive [state] to simulate playback. The Android-only members (video surface,
 * ad container) are never used by the ViewModel logic, so they throw.
 */
class RecordingPlayerController(override val isCastAvailable: Boolean = false) : PlayerController {

    override val state = MutableStateFlow(PlayerState())
    override val player: Player get() = throw UnsupportedOperationException("not used in unit tests")
    override val adViewContainer: ViewGroup get() = throw UnsupportedOperationException("not used in unit tests")

    val commands = mutableListOf<String>()
    var released = false
        private set

    override fun load(request: PlaybackRequest) {
        commands += "load:${request.id}"
        state.value = PlayerState(request = request, playWhenReady = true)
    }

    override fun play() { commands += "play" }
    override fun pause() { commands += "pause" }
    override fun togglePlayPause() { commands += "toggle" }
    override fun seekTo(positionMs: Long) { commands += "seekTo:$positionMs" }
    override fun seekForward(incrementMs: Long) { commands += "forward:$incrementMs" }
    override fun seekBack(incrementMs: Long) { commands += "back:$incrementMs" }
    override fun setQuality(qualityId: String?) { commands += "quality:$qualityId" }
    override fun setAudioTrack(trackId: String) { commands += "audio:$trackId" }
    override fun setSubtitleTrack(trackId: String?) { commands += "subtitle:$trackId" }
    override fun setSpeed(speed: Float) { commands += "speed:$speed" }
    override fun goToLive() { commands += "live" }
    override fun retry() { commands += "retry" }

    override fun release() {
        released = true
    }

    fun update(transform: (PlayerState) -> PlayerState) {
        state.value = transform(state.value)
    }
}
