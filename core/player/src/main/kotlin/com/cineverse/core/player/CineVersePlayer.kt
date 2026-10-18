package com.cineverse.core.player

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ads.AdsLoader
import androidx.media3.common.AdViewProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.cineverse.core.player.analytics.PlaybackAnalyticsListener
import com.cineverse.core.player.cast.CastSupport
import com.cineverse.core.player.internal.MediaItemFactory
import com.cineverse.core.player.internal.TrackMapper
import com.cineverse.core.player.internal.TrackSnapshot
import com.cineverse.core.player.model.ContentType
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlaybackStatus
import com.cineverse.core.player.model.PlayerError
import com.cineverse.core.player.model.PlayerState
import com.cineverse.core.player.model.VideoQuality
import com.cineverse.core.player.session.ActiveSessionHolder
import com.cineverse.core.player.session.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Media3 implementation of [PlayerController]:
 * - ExoPlayer with HLS/DASH, audio focus, becoming-noisy handling and a network wake lock.
 * - IMA client-side ads when [PlaybackRequest.adTagUrl] is set.
 * - CastPlayer wrapping the local player when Cast is available, so handoff keeps position.
 * - A MediaSession published through [PlaybackService] for system media controls.
 *
 * Features get it through [PlayerControllerFactory] and call [release] when done.
 */
internal class CineVersePlayer private constructor(
    private val context: Context,
    private val analyticsListeners: Set<PlaybackAnalyticsListener>,
) : PlayerController {

    private val scope = MainScope()

    private val adsLoader: ImaAdsLoader = ImaAdsLoader.Builder(context).build()

    override val adViewContainer: ViewGroup = FrameLayout(context)

    private val adViewProvider = object : AdViewProvider {
        override fun getAdViewGroup(): ViewGroup = adViewContainer
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setLocalAdInsertionComponents(AdsLoader.Provider { adsLoader }, adViewProvider),
        )
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        // The default track selection parameters already follow the system caption settings
        // (CaptioningManager), so subtitles start in the user's preferred language when enabled.
        .build()

    override val isCastAvailable: Boolean = CastSupport.isAvailable(context)

    override val player: Player =
        if (isCastAvailable) {
            CastPlayer.Builder(context).setLocalPlayer(exoPlayer).build()
        } else {
            exoPlayer
        }

    private val mediaSession: MediaSession = MediaSession.Builder(context, player)
        .setId("cineverse-${UUID.randomUUID()}")
        .apply { sessionActivity()?.let(::setSessionActivity) }
        .build()

    private val sessionControllerFuture = run {
        ActiveSessionHolder.session = mediaSession
        // Connecting a controller binds PlaybackService, which then publishes the session.
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java)),
        ).buildAsync()
    }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var tracks: TrackSnapshot = TrackSnapshot.EMPTY
    private var currentVideoHeight: Int? = null
    private var playbackStartedReported = false
    private var bufferingStartedAtMs: Long? = null
    private var released = false

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_TRACK_SELECTION_PARAMETERS_CHANGED)) {
                tracks = TrackMapper.map(player.currentTracks, player.trackSelectionParameters)
            }
            updateState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val request = _state.value.request ?: return
            if (playbackState == Player.STATE_BUFFERING && !player.isPlayingAd) {
                bufferingStartedAtMs = SystemClock.elapsedRealtime()
                analyticsListeners.forEach { it.onBufferingStarted(request, player.currentPosition) }
            } else {
                bufferingStartedAtMs?.let { started ->
                    val duration = SystemClock.elapsedRealtime() - started
                    analyticsListeners.forEach { it.onBufferingEnded(request, player.currentPosition, duration) }
                }
                bufferingStartedAtMs = null
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val request = _state.value.request ?: return
            if (isPlaying && !player.isPlayingAd && !playbackStartedReported) {
                playbackStartedReported = true
                analyticsListeners.forEach { it.onPlaybackStarted(request) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                // Paused too long on a live stream: rejoin the live window instead of failing.
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            val request = _state.value.request ?: return
            val playerError = error.toPlayerError()
            analyticsListeners.forEach { it.onPlaybackError(request, playerError) }
        }
    }

    private val exoAnalyticsListener = object : AnalyticsListener {
        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?,
        ) {
            if (format.height <= 0) return
            currentVideoHeight = format.height
            val request = _state.value.request ?: return
            if (exoPlayer.isPlayingAd) return
            val quality = VideoQuality(format.id.orEmpty(), format.width, format.height, format.bitrate)
            analyticsListeners.forEach { it.onQualityChanged(request, quality) }
        }
    }

    init {
        adsLoader.setPlayer(exoPlayer)
        player.addListener(playerListener)
        exoPlayer.addAnalyticsListener(exoAnalyticsListener)
        startTicker()
    }

    override fun load(request: PlaybackRequest) {
        if (_state.value.request?.id == request.id) return
        playbackStartedReported = false
        val mediaItem = MediaItemFactory.create(request)
        if (request.type == ContentType.LIVE || request.startPositionMs <= 0L) {
            player.setMediaItem(mediaItem)
        } else {
            player.setMediaItem(mediaItem, request.startPositionMs)
        }
        player.playbackParameters = player.playbackParameters.withSpeed(1f)
        player.prepare()
        player.playWhenReady = true
        _state.value = PlayerState(request = request, status = PlaybackStatus.Buffering, playWhenReady = true)
    }

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    override fun pause() = player.pause()

    override fun togglePlayPause() {
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) pause() else play()
    }

    override fun seekTo(positionMs: Long) {
        if (!_state.value.canSeek) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET }
        player.seekTo(positionMs.coerceIn(0L, duration ?: Long.MAX_VALUE))
    }

    override fun seekForward(incrementMs: Long) = seekTo(player.currentPosition + incrementMs)

    override fun seekBack(incrementMs: Long) = seekTo(player.currentPosition - incrementMs)

    override fun setQuality(qualityId: String?) {
        val ref = qualityId?.let { tracks.refs[it] ?: return }
        player.trackSelectionParameters = TrackMapper.override(player.trackSelectionParameters, C.TRACK_TYPE_VIDEO, ref)
    }

    override fun setAudioTrack(trackId: String) {
        val ref = tracks.refs[trackId] ?: return
        player.trackSelectionParameters = TrackMapper.override(player.trackSelectionParameters, C.TRACK_TYPE_AUDIO, ref)
            .buildUpon()
            // Also prefer the language, so the choice carries over to the next title.
            .setPreferredAudioLanguage(ref.group.getFormat(ref.index).language)
            .build()
    }

    override fun setSubtitleTrack(trackId: String?) {
        val params = player.trackSelectionParameters
        player.trackSelectionParameters = if (trackId == null) {
            params.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
        } else {
            val ref = tracks.refs[trackId] ?: return
            TrackMapper.override(params, C.TRACK_TYPE_TEXT, ref)
        }
    }

    override fun setSpeed(speed: Float) {
        if (!_state.value.canChangeSpeed) return
        player.setPlaybackSpeed(speed.coerceIn(MIN_SPEED, MAX_SPEED))
    }

    override fun goToLive() {
        if (player.isCurrentMediaItemLive) {
            player.seekToDefaultPosition()
            player.play()
        }
    }

    override fun retry() {
        if (player.playerError?.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW || player.isCurrentMediaItemLive) {
            player.seekToDefaultPosition()
        }
        player.prepare()
        player.play()
    }

    override fun release() {
        if (released) return
        released = true
        scope.cancel()
        MediaController.releaseFuture(sessionControllerFuture)
        if (ActiveSessionHolder.session === mediaSession) ActiveSessionHolder.session = null
        mediaSession.release()
        player.removeListener(playerListener)
        exoPlayer.removeAnalyticsListener(exoAnalyticsListener)
        adsLoader.setPlayer(null)
        adsLoader.release()
        // CastPlayer.release() also releases the local ExoPlayer it wraps.
        player.release()
    }

    private fun startTicker() {
        scope.launch {
            var sinceProgressMs = 0L
            while (isActive) {
                delay(TICK_MS)
                updateState()
                if (player.isPlaying && !player.isPlayingAd) {
                    sinceProgressMs += TICK_MS
                    if (sinceProgressMs >= PlaybackAnalyticsListener.PROGRESS_INTERVAL_MS) {
                        sinceProgressMs = 0L
                        reportProgress()
                    }
                }
            }
        }
    }

    private fun updateState() {
        if (released) return
        val p = player
        val duration = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        val isLive = p.isCurrentMediaItemLive
        val videoSize = p.videoSize
        val isCasting = p.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
        val liveBehindMs = if (isLive) liveBehindMs(p) else 0L
        _state.value = _state.value.copy(
            status = when {
                p.playerError != null -> PlaybackStatus.Error
                p.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
                p.playbackState == Player.STATE_READY -> PlaybackStatus.Ready
                p.playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
                else -> PlaybackStatus.Idle
            },
            isPlaying = p.isPlaying,
            playWhenReady = p.playWhenReady,
            positionMs = p.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            bufferedPositionMs = p.bufferedPosition.coerceAtLeast(0L),
            isLive = isLive,
            isSeekable = p.isCurrentMediaItemSeekable,
            isAtLiveEdge = isLive && liveBehindMs <= LIVE_EDGE_TOLERANCE_MS,
            liveBehindMs = liveBehindMs,
            isPlayingAd = p.isPlayingAd,
            videoQualities = tracks.videoQualities,
            selectedVideoQualityId = tracks.selectedVideoQualityId,
            currentVideoHeight = currentVideoHeight,
            audioTracks = tracks.audioTracks,
            selectedAudioTrackId = tracks.selectedAudioTrackId,
            subtitleTracks = tracks.subtitleTracks,
            selectedSubtitleTrackId = tracks.selectedSubtitleTrackId,
            playbackSpeed = p.playbackParameters.speed,
            videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
            videoHeight = videoSize.height,
            isCasting = isCasting,
            castDeviceName = if (isCasting) CastSupport.currentDeviceName(context) else null,
            error = p.playerError?.toPlayerError(),
        )
    }

    /**
     * Distance from the live edge. Players intentionally stay a few segments behind the end of
     * the live window, so the "edge" is the window's default position, not its end.
     */
    private fun liveBehindMs(p: Player): Long {
        val timeline = p.currentTimeline
        if (timeline.isEmpty) return 0L
        val window = timeline.getWindow(p.currentMediaItemIndex, Timeline.Window())
        return (window.defaultPositionMs - p.currentPosition).coerceAtLeast(0L)
    }

    /** Analytics progress only; watch progress for the backend ([com.cineverse.core.player.analytics.ProgressReporter]) is reported by the screen. */
    private fun reportProgress() {
        val request = _state.value.request ?: return
        val p = player
        if (p.isCurrentMediaItemLive || p.isPlayingAd) return
        val duration = p.duration.takeIf { it != C.TIME_UNSET } ?: return
        analyticsListeners.forEach { it.onProgress(request, p.currentPosition, duration) }
    }

    private fun sessionActivity(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun PlaybackException.toPlayerError() = PlayerError(errorCode, errorCodeName, message)

    class Factory @Inject constructor(
        @param:ApplicationContext private val context: Context,
        private val analyticsListeners: Set<@JvmSuppressWildcards PlaybackAnalyticsListener>,
    ) : PlayerControllerFactory {
        override fun create(): PlayerController = CineVersePlayer(context, analyticsListeners)
    }

    private companion object {
        const val TICK_MS = 500L
        const val LIVE_EDGE_TOLERANCE_MS = 10_000L
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 2f
    }
}
