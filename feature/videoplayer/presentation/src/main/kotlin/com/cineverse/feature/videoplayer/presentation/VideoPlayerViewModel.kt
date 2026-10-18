package com.cineverse.feature.videoplayer.presentation

import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.cineverse.core.player.PlayerController
import com.cineverse.core.player.PlayerControllerFactory
import com.cineverse.core.player.TrackLabels
import com.cineverse.core.player.analytics.PlaybackAnalyticsListener
import com.cineverse.core.player.analytics.ProgressReporter
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlaybackStatus
import com.cineverse.core.player.model.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Screen logic for the video player. Owns one [PlayerController] per screen, so rotation,
 * fullscreen, resizing and PiP never recreate the player or lose the position. Maps the player
 * state and the screen state into [uiState], handles control auto-hide, the settings panel,
 * fullscreen and PiP flags, retry, and reports watch progress to the [ProgressReporter] hooks.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    playerFactory: PlayerControllerFactory,
    private val progressReporters: Set<@JvmSuppressWildcards ProgressReporter>,
) : ViewModel() {

    private val controller: PlayerController = playerFactory.create()

    /** The player to attach to the video surface and subtitle view. */
    val player: Player get() = controller.player

    /** IMA ad UI container; the screen must host it over the video. */
    val adViewContainer: ViewGroup get() = controller.adViewContainer

    private val screen = MutableStateFlow(ScreenState())

    val uiState: StateFlow<VideoPlayerUiState> = combine(controller.state, screen, ::toUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, toUiState(controller.state.value, screen.value))

    private var autoHideJob: Job? = null
    private var seekFeedbackJob: Job? = null
    private var progressJob: Job? = null

    init {
        viewModelScope.launch {
            var previous = controller.state.value
            controller.state.collect { current ->
                onPlayerStateChanged(previous, current)
                previous = current
            }
        }
    }

    fun onAction(action: VideoPlayerAction) {
        val playerState = controller.state.value
        when (action) {
            is VideoPlayerAction.Load -> {
                screen.update { it.copy(request = action.request) }
                controller.load(action.request)
            }

            VideoPlayerAction.PlayPauseClicked -> interacted { controller.togglePlayPause() }
            VideoPlayerAction.PlayClicked -> controller.play()
            VideoPlayerAction.PauseClicked -> controller.pause()
            is VideoPlayerAction.SeekForwardClicked -> interacted { controller.seekForward(action.incrementMs) }
            is VideoPlayerAction.SeekBackClicked -> interacted { controller.seekBack(action.incrementMs) }
            is VideoPlayerAction.SeekTo -> interacted { controller.seekTo(action.positionMs) }
            is VideoPlayerAction.Scrubbing -> {
                screen.update { it.copy(scrubPositionMs = action.positionMs) }
                scheduleAutoHide()
            }

            is VideoPlayerAction.DoubleTapped -> doubleTapSeek(action, playerState)
            VideoPlayerAction.GoToLiveClicked -> interacted { controller.goToLive() }
            VideoPlayerAction.RetryClicked -> controller.retry()

            VideoPlayerAction.VideoTapped -> interacted {
                screen.update { it.copy(controlsShown = !it.controlsShown) }
            }

            VideoPlayerAction.FullscreenToggled -> interacted {
                screen.update { it.copy(userFullscreen = !it.isFullscreen) }
            }

            VideoPlayerAction.SettingsOpened -> interacted {
                screen.update { it.copy(settingsOpen = true, settingsPage = SettingsPage.Main) }
            }

            VideoPlayerAction.SettingsDismissed -> closeSettings()
            is VideoPlayerAction.SettingsPageOpened -> screen.update { it.copy(settingsPage = action.page) }
            VideoPlayerAction.SettingsBackPressed ->
                if (screen.value.settingsPage == SettingsPage.Main) {
                    closeSettings()
                } else {
                    screen.update { it.copy(settingsPage = SettingsPage.Main) }
                }

            is VideoPlayerAction.QualitySelected -> selected { controller.setQuality(action.qualityId) }
            is VideoPlayerAction.AudioSelected -> selected { controller.setAudioTrack(action.trackId) }
            is VideoPlayerAction.SubtitleSelected -> selected { controller.setSubtitleTrack(action.trackId) }
            is VideoPlayerAction.SpeedSelected -> selected { controller.setSpeed(action.speed) }

            is VideoPlayerAction.WindowChanged -> screen.update {
                it.copy(isLargeScreen = action.isLargeScreen, isLandscape = action.isLandscape)
            }

            is VideoPlayerAction.PictureInPictureChanged ->
                screen.update { it.copy(isInPictureInPicture = action.isInPictureInPicture) }

            is VideoPlayerAction.AccessibilityChanged -> {
                screen.update {
                    it.copy(touchExplorationEnabled = action.touchExplorationEnabled, autoHideDelayMs = action.controlsTimeoutMs)
                }
                scheduleAutoHide()
            }

            // The TV keeps playing while casting, so leaving the app must not pause it.
            VideoPlayerAction.AppBackgrounded -> if (!playerState.isCasting) controller.pause()
        }
    }

    override fun onCleared() {
        reportProgress(controller.state.value, isFinal = true)
        controller.release()
    }

    private inline fun interacted(block: () -> Unit) {
        block()
        scheduleAutoHide()
    }

    /** Applies a settings choice to the running player and returns to the main settings page. */
    private inline fun selected(block: () -> Unit) {
        block()
        screen.update { it.copy(settingsPage = SettingsPage.Main) }
    }

    private fun closeSettings() {
        screen.update { it.copy(settingsOpen = false, settingsPage = SettingsPage.Main) }
        scheduleAutoHide()
    }

    private fun doubleTapSeek(action: VideoPlayerAction.DoubleTapped, playerState: PlayerState) {
        if (!playerState.canSeek) return
        if (action.forward) controller.seekForward(action.incrementMs) else controller.seekBack(action.incrementMs)
        screen.update { current ->
            val previous = current.seekFeedback?.takeIf { it.forward == action.forward }?.seconds ?: 0
            current.copy(seekFeedback = SeekFeedback(action.forward, previous + (action.incrementMs / 1000).toInt()))
        }
        seekFeedbackJob?.cancel()
        seekFeedbackJob = viewModelScope.launch {
            delay(SEEK_FEEDBACK_MS)
            screen.update { it.copy(seekFeedback = null) }
        }
    }

    /**
     * Restarts the auto-hide timer. Controls hide only while playing, and never while TalkBack
     * is on, settings are open or the user is scrubbing.
     */
    private fun scheduleAutoHide() {
        autoHideJob?.cancel()
        val s = screen.value
        val canHide = s.controlsShown && controller.state.value.isPlaying && !s.settingsOpen &&
            s.scrubPositionMs == null && !s.touchExplorationEnabled
        if (!canHide) return
        autoHideJob = viewModelScope.launch {
            delay(s.autoHideDelayMs)
            screen.update { it.copy(controlsShown = false) }
        }
    }

    private fun onPlayerStateChanged(previous: PlayerState, current: PlayerState) {
        if (previous.isPlaying != current.isPlaying) scheduleAutoHide()

        val wasPlayingContent = previous.isPlaying && !previous.isPlayingAd
        val isPlayingContent = current.isPlaying && !current.isPlayingAd
        if (isPlayingContent != wasPlayingContent) {
            progressJob?.cancel()
            if (isPlayingContent) {
                progressJob = viewModelScope.launch {
                    while (true) {
                        delay(PlaybackAnalyticsListener.PROGRESS_INTERVAL_MS)
                        reportProgress(controller.state.value, isFinal = false)
                    }
                }
            }
        }
        val paused = previous.isPlaying && !current.isPlaying && current.status == PlaybackStatus.Ready
        val ended = previous.status != PlaybackStatus.Ended && current.status == PlaybackStatus.Ended
        if (paused || ended) reportProgress(current, isFinal = true)
    }

    private fun reportProgress(state: PlayerState, isFinal: Boolean) {
        val request = state.request ?: return
        if (state.isLive || state.isPlayingAd || state.durationMs <= 0L) return
        progressReporters.forEach { it.reportProgress(request, state.positionMs, state.durationMs, isFinal) }
    }

    private fun toUiState(player: PlayerState, screen: ScreenState): VideoPlayerUiState {
        val request = screen.request ?: player.request
        val displayPosition = screen.scrubPositionMs ?: player.positionMs
        val isEnded = player.status == PlaybackStatus.Ended
        return VideoPlayerUiState(
            title = request?.title.orEmpty(),
            subtitle = request?.subtitle,
            isPlayRequested = player.playWhenReady && !isEnded,
            isPlaying = player.isPlaying,
            isBuffering = player.isBuffering && player.playWhenReady,
            isEnded = isEnded,
            positionMs = player.positionMs,
            displayPositionMs = displayPosition,
            durationMs = player.durationMs,
            bufferedPositionMs = player.bufferedPositionMs,
            canSeek = player.canSeek,
            isLive = player.isLive,
            isAtLiveEdge = player.isAtLiveEdge,
            liveBehindMs = if (player.isLive) (player.liveBehindMs + player.positionMs - displayPosition).coerceAtLeast(0L) else 0L,
            isPlayingAd = player.isPlayingAd,
            isCasting = player.isCasting,
            castDeviceName = player.castDeviceName,
            showCastButton = controller.isCastAvailable,
            videoWidth = player.videoWidth,
            videoHeight = player.videoHeight,
            controlsVisible = screen.controlsShown || screen.touchExplorationEnabled || isEnded,
            isFullscreen = screen.isFullscreen,
            isInPictureInPicture = screen.isInPictureInPicture,
            canEnterPictureInPicture = !player.isCasting,
            settings = settingsUiState(player, screen),
            seekFeedback = screen.seekFeedback,
            error = player.error?.let { PlayerErrorUiState(it.message ?: it.codeName) },
        )
    }

    private fun settingsUiState(player: PlayerState, screen: ScreenState): SettingsUiState {
        val audioNames = TrackLabels.audioNames(player.audioTracks)
        val subtitleNames = TrackLabels.subtitleNames(player.subtitleTracks)
        return SettingsUiState(
            isVisible = screen.settingsOpen && !player.isPlayingAd && !screen.isInPictureInPicture,
            asSidePanel = screen.isLandscape || screen.isLargeScreen,
            page = screen.settingsPage,
            qualities = listOf(QualityOption(null, null, player.selectedVideoQualityId == null)) +
                player.videoQualities.map { QualityOption(it.id, it.height, it.id == player.selectedVideoQualityId) },
            autoQualityHeight = player.currentVideoHeight,
            audioTracks = player.audioTracks.map {
                TrackOption(it.id, audioNames.getValue(it.id), it.id == player.selectedAudioTrackId)
            },
            subtitleTracks = listOf(TrackOption(null, null, player.selectedSubtitleTrackId == null)) +
                player.subtitleTracks.map {
                    TrackOption(it.id, subtitleNames.getValue(it.id), it.id == player.selectedSubtitleTrackId)
                },
            speeds = SPEEDS.map { SpeedOption(it, it == player.playbackSpeed) },
            currentSpeed = player.playbackSpeed,
            canChangeSpeed = player.canChangeSpeed,
        )
    }

    /** Screen-only state that the player knows nothing about. */
    private data class ScreenState(
        val request: PlaybackRequest? = null,
        val controlsShown: Boolean = true,
        val scrubPositionMs: Long? = null,
        val userFullscreen: Boolean = false,
        val isLargeScreen: Boolean = false,
        val isLandscape: Boolean = false,
        val isInPictureInPicture: Boolean = false,
        val touchExplorationEnabled: Boolean = false,
        val autoHideDelayMs: Long = DEFAULT_AUTO_HIDE_DELAY_MS,
        val settingsOpen: Boolean = false,
        val settingsPage: SettingsPage = SettingsPage.Main,
        val seekFeedback: SeekFeedback? = null,
    ) {
        /** Phones go fullscreen in landscape; tablets and unfolded foldables only on request. */
        val isFullscreen: Boolean get() = userFullscreen || (!isLargeScreen && isLandscape)
    }

    companion object {
        const val DEFAULT_AUTO_HIDE_DELAY_MS = 4_000L
        const val SEEK_FEEDBACK_MS = 700L
        val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    }
}
