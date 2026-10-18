package com.cineverse.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.cineverse.core.analytics.AnalyticsEvent
import com.cineverse.core.analytics.AnalyticsTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Thin wrapper around Media3 [ExoPlayer] used by both apps. It handles HLS VOD and live streams
 * and exposes playback as a [StateFlow]. Create one per playback screen through [Factory] and
 * call [release] when that screen goes away.
 */
class CineVersePlayer private constructor(
    context: Context,
    private val analytics: AnalyticsTracker,
) {
    /** Exposed for the video surface only; drive playback through this class's methods. */
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val status = when (playbackState) {
                Player.STATE_BUFFERING -> PlaybackState.Status.Buffering
                Player.STATE_READY -> PlaybackState.Status.Ready
                Player.STATE_ENDED -> PlaybackState.Status.Ended
                else -> PlaybackState.Status.Idle
            }
            _state.update { it.copy(status = status, isAtLiveEdge = isAtLiveEdge()) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying, isAtLiveEdge = isAtLiveEdge()) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(status = PlaybackState.Status.Error, errorMessage = error.message) }
            analytics.track(
                AnalyticsEvent(
                    AnalyticsEvent.Types.PLAYBACK_ERROR,
                    mapOf(AnalyticsEvent.Params.ERROR_CODE to error.errorCodeName),
                ),
            )
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    /** Loads [media] and starts playback. Live streams start at the live edge. */
    fun load(media: PlayableMedia, playWhenReady: Boolean = true) {
        if (_state.value.media?.id == media.id) return
        val mediaItem = MediaItem.Builder()
            .setMediaId(media.id)
            .setUri(media.streamUrl)
            .setMimeType(MimeTypes.APPLICATION_M3U8)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(media.title).build())
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = playWhenReady
        _state.value = PlaybackState(media = media, status = PlaybackState.Status.Buffering)
        analytics.track(
            AnalyticsEvent(
                AnalyticsEvent.Types.PLAYBACK_STARTED,
                mapOf(
                    AnalyticsEvent.Params.CONTENT_ID to media.id,
                    AnalyticsEvent.Params.IS_LIVE to media.isLive.toString(),
                ),
            ),
        )
    }

    fun play() = exoPlayer.play()

    fun pause() = exoPlayer.pause()

    fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)

    /** Jumps back to the live edge after the viewer paused or scrubbed back in a live stream. */
    fun seekToLiveEdge() {
        if (exoPlayer.isCurrentMediaItemLive) exoPlayer.seekToDefaultPosition()
    }

    /** Clears the error and tries the current item again. */
    fun retry() {
        _state.update { it.copy(status = PlaybackState.Status.Buffering, errorMessage = null) }
        exoPlayer.prepare()
    }

    fun release() {
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }

    @OptIn(UnstableApi::class)
    private fun isAtLiveEdge(): Boolean {
        if (!exoPlayer.isCurrentMediaItemLive) return false
        val offset = exoPlayer.currentLiveOffset
        return offset != C.TIME_UNSET && offset <= LIVE_EDGE_TOLERANCE_MS
    }

    class Factory @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val analytics: AnalyticsTracker,
    ) {
        fun create(): CineVersePlayer = CineVersePlayer(context, analytics)
    }

    private companion object {
        const val LIVE_EDGE_TOLERANCE_MS = 15_000L
    }
}
