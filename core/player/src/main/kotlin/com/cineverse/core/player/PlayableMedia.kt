package com.cineverse.core.player

/**
 * What the player needs to start playback. Kept separate from the domain model so this module
 * depends only on Media3; the app maps catalog items to it.
 */
data class PlayableMedia(
    val id: String,
    val title: String,
    val streamUrl: String,
    val isLive: Boolean,
)
