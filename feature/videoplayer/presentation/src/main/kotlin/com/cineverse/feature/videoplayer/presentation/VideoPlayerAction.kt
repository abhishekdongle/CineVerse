package com.cineverse.feature.videoplayer.presentation

import com.cineverse.core.player.model.PlaybackRequest

/** User actions and screen events sent to [VideoPlayerViewModel.onAction]. */
sealed interface VideoPlayerAction {

    /** Starts [request]. Sending the same request again does nothing. */
    data class Load(val request: PlaybackRequest) : VideoPlayerAction

    // Playback
    data object PlayPauseClicked : VideoPlayerAction
    data object PlayClicked : VideoPlayerAction
    data object PauseClicked : VideoPlayerAction
    data class SeekForwardClicked(val incrementMs: Long) : VideoPlayerAction
    data class SeekBackClicked(val incrementMs: Long) : VideoPlayerAction

    /** Seek bar released at [positionMs]. */
    data class SeekTo(val positionMs: Long) : VideoPlayerAction

    /** Seek bar dragged to [positionMs]; null when the drag ends. Previews without seeking. */
    data class Scrubbing(val positionMs: Long?) : VideoPlayerAction

    /** Double-tap on the right ([forward]) or left half of the video. */
    data class DoubleTapped(val forward: Boolean, val incrementMs: Long) : VideoPlayerAction
    data object GoToLiveClicked : VideoPlayerAction
    data object RetryClicked : VideoPlayerAction

    // Controls and settings
    /** Single tap on the video: shows or hides the controls. */
    data object VideoTapped : VideoPlayerAction
    data object FullscreenToggled : VideoPlayerAction
    data object SettingsOpened : VideoPlayerAction
    data object SettingsDismissed : VideoPlayerAction
    data class SettingsPageOpened(val page: SettingsPage) : VideoPlayerAction

    /** Back inside the settings panel: a sub-page returns to the main page, the main page closes it. */
    data object SettingsBackPressed : VideoPlayerAction
    data class QualitySelected(val qualityId: String?) : VideoPlayerAction
    data class AudioSelected(val trackId: String) : VideoPlayerAction
    data class SubtitleSelected(val trackId: String?) : VideoPlayerAction
    data class SpeedSelected(val speed: Float) : VideoPlayerAction

    // Environment reported by the UI
    data class WindowChanged(val isLargeScreen: Boolean, val isLandscape: Boolean) : VideoPlayerAction
    data class PictureInPictureChanged(val isInPictureInPicture: Boolean) : VideoPlayerAction

    /**
     * TalkBack state and the accessibility-adjusted auto-hide delay. While touch exploration
     * is on, controls never auto-hide.
     */
    data class AccessibilityChanged(val touchExplorationEnabled: Boolean, val controlsTimeoutMs: Long) : VideoPlayerAction

    /** The app went to the background (not PiP). Pauses unless casting. */
    data object AppBackgrounded : VideoPlayerAction
}
