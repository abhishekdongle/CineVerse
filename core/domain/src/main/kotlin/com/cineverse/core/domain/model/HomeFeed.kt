package com.cineverse.core.domain.model

data class HomeFeed(
    val featured: MediaItem?,
    val rails: List<Rail>,
)

/** A horizontal row on the home screen, e.g. "Live now" or "Trending". */
data class Rail(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
)
