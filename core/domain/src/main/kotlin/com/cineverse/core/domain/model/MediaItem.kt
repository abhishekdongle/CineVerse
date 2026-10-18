package com.cineverse.core.domain.model

/** A playable title in the catalog: a VOD asset or a live channel. */
data class MediaItem(
    val id: String,
    val title: String,
    /** Short secondary line, e.g. "2012 · Sci-fi". */
    val subtitle: String?,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val contentType: ContentType,
    val access: AccessModel,
    /** VAST/VMAP ad tag for ad-supported titles; null plays without ads. */
    val adTagUrl: String?,
)

enum class ContentType {
    /** On-demand asset with a fixed duration. */
    VOD,

    /** Linear channel or event; playback starts at the live edge. */
    LIVE,

    /** Short-form video: trailers, highlights, previews. */
    CLIP,
}

enum class AccessModel {
    /** Requires an active subscription (SVOD). */
    SUBSCRIPTION,

    /** Free to watch, monetized with ads (AVOD). */
    AD_SUPPORTED,
}
