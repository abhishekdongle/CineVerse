package com.cineverse.feature.videoplayer.presentation

/**
 * Everything the video player screen renders, as one immutable snapshot. Built by
 * [VideoPlayerViewModel] from the player state and the screen state (controls, settings,
 * fullscreen, PiP). Text that needs string resources ("Auto", "Off", "1.5×") is formatted by
 * the UI from these values.
 */
data class VideoPlayerUiState(
    val title: String = "",
    val subtitle: String? = null,
    /** The user wants playback (pause icon shown); stays true while buffering. */
    val isPlayRequested: Boolean = false,
    val isPlaying: Boolean = false,
    /** Show the loading spinner: buffering while the user wants playback. */
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    /** Position to show in the time label: the scrub position while dragging, else [positionMs]. */
    val displayPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val canSeek: Boolean = false,
    val isLive: Boolean = false,
    val isAtLiveEdge: Boolean = false,
    /** Distance behind the live edge at [displayPositionMs]; 0 when not live. */
    val liveBehindMs: Long = 0L,
    val isPlayingAd: Boolean = false,
    val isCasting: Boolean = false,
    val castDeviceName: String? = null,
    val showCastButton: Boolean = false,
    /** Display size of the video in pixels; 0 until known. */
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    /** Controls overlay shown (always while TalkBack is on or playback has ended). */
    val controlsVisible: Boolean = true,
    val isFullscreen: Boolean = false,
    val isInPictureInPicture: Boolean = false,
    /** PiP is offered only while not casting. */
    val canEnterPictureInPicture: Boolean = false,
    val settings: SettingsUiState = SettingsUiState(),
    val seekFeedback: SeekFeedback? = null,
    val error: PlayerErrorUiState? = null,
) {
    /** A landscape (or not yet known) video rotates the phone to landscape in fullscreen. */
    val isLandscapeVideo: Boolean
        get() = videoWidth == 0 || videoWidth >= videoHeight
}

enum class SettingsPage { Main, Quality, Audio, Subtitles, Speed }

/** Quality, audio, subtitle and speed settings. Option lists already mark the selected entry. */
data class SettingsUiState(
    val isVisible: Boolean = false,
    /** Side panel (landscape or large screens) instead of a bottom sheet. */
    val asSidePanel: Boolean = false,
    val page: SettingsPage = SettingsPage.Main,
    /** First entry is "Auto" (id and height null). */
    val qualities: List<QualityOption> = emptyList(),
    /** Height currently playing, shown next to "Auto". */
    val autoQualityHeight: Int? = null,
    val audioTracks: List<TrackOption> = emptyList(),
    /** First entry is "Off" (id and label null). */
    val subtitleTracks: List<TrackOption> = emptyList(),
    val speeds: List<SpeedOption> = emptyList(),
    val currentSpeed: Float = 1f,
    val canChangeSpeed: Boolean = false,
) {
    val canChangeQuality: Boolean get() = qualities.size > 1
    val canChangeAudio: Boolean get() = audioTracks.size > 1
    val canChangeSubtitles: Boolean get() = subtitleTracks.size > 1
    val selectedQuality: QualityOption? get() = qualities.firstOrNull { it.isSelected }
    val selectedAudio: TrackOption? get() = audioTracks.firstOrNull { it.isSelected }
    val selectedSubtitle: TrackOption? get() = subtitleTracks.firstOrNull { it.isSelected }
}

data class QualityOption(val id: String?, val height: Int?, val isSelected: Boolean)

/** [label] is a ready-to-show track name ("English (5.1)"); null only for the "Off" entry. */
data class TrackOption(val id: String?, val label: String?, val isSelected: Boolean)

data class SpeedOption(val speed: Float, val isSelected: Boolean)

/** Brief "+20s" bubble after double-tapping one side of the video. */
data class SeekFeedback(val forward: Boolean, val seconds: Int)

data class PlayerErrorUiState(val message: String)
