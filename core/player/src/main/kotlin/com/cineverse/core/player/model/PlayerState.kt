package com.cineverse.core.player.model

/** Snapshot of the player, emitted by [com.cineverse.core.player.PlayerController.state]. */
data class PlayerState(
    val request: PlaybackRequest? = null,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val isPlaying: Boolean = false,
    /** True when the user wants playback; stays true while buffering. */
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0L,
    /** Content duration, or the seekable window length for live streams. */
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isLive: Boolean = false,
    val isSeekable: Boolean = false,
    val isAtLiveEdge: Boolean = false,
    /** How far playback is behind the live edge; 0 when at the edge or not live. */
    val liveBehindMs: Long = 0L,
    val isPlayingAd: Boolean = false,
    val videoQualities: List<VideoQuality> = emptyList(),
    /** Null means adaptive ("Auto"). */
    val selectedVideoQualityId: String? = null,
    /** Height of the rendition currently playing, shown next to "Auto". */
    val currentVideoHeight: Int? = null,
    val audioTracks: List<AudioTrackOption> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val subtitleTracks: List<SubtitleTrackOption> = emptyList(),
    /** Null means subtitles are off. */
    val selectedSubtitleTrackId: String? = null,
    val playbackSpeed: Float = 1f,
    /** Display size of the video in pixels (pixel aspect ratio applied); 0 until known. */
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isCasting: Boolean = false,
    val castDeviceName: String? = null,
    val error: PlayerError? = null,
) {
    /** Live streams are seekable only when the DVR window is long enough to be useful. */
    val canSeek: Boolean
        get() = isSeekable && !isPlayingAd && (!isLive || durationMs >= MIN_DVR_WINDOW_MS)

    val canChangeSpeed: Boolean
        get() = !isLive && !isPlayingAd

    val isBuffering: Boolean
        get() = status == PlaybackStatus.Buffering

    companion object {
        const val MIN_DVR_WINDOW_MS = 60_000L
    }
}

enum class PlaybackStatus { Idle, Buffering, Ready, Ended, Error }

data class VideoQuality(
    val id: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
)

data class AudioTrackOption(
    val id: String,
    val language: String?,
    val label: String?,
    val channelCount: Int,
)

data class SubtitleTrackOption(
    val id: String,
    val language: String?,
    val label: String?,
    val isForced: Boolean,
    val isClosedCaption: Boolean,
)

data class PlayerError(
    val code: Int,
    val codeName: String,
    val message: String?,
)
