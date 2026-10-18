package com.cineverse.core.domain.model

data class PlaybackSettings(
    val autoplayNextEpisode: Boolean,
    /** Caps video quality on metered connections. */
    val dataSaver: Boolean,
)
