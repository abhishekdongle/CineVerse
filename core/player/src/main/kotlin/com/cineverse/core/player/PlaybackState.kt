package com.cineverse.core.player

data class PlaybackState(
    val media: PlayableMedia? = null,
    val status: Status = Status.Idle,
    val isPlaying: Boolean = false,
    /** True when a live stream is playing at (or very near) the live edge. */
    val isAtLiveEdge: Boolean = false,
    val errorMessage: String? = null,
) {
    enum class Status { Idle, Buffering, Ready, Ended, Error }
}
