package com.cineverse.core.player.model

/** Everything needed to start playback of one title. UI modules pass this to [com.cineverse.core.player.PlayerController.load]. */
data class PlaybackRequest(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    /** HLS (.m3u8) or DASH (.mpd) manifest URL. */
    val streamUrl: String,
    val type: ContentType,
    /** Ignored for [ContentType.LIVE], which always starts at the live edge. */
    val startPositionMs: Long = 0L,
    /** VAST or VMAP ad tag for AVOD; null plays without ads. */
    val adTagUrl: String? = null,
    val drm: DrmConfig? = null,
    val sideloadedSubtitles: List<SubtitleTrack> = emptyList(),
    /** WebVTT thumbnail track for seek previews. Not used yet. */
    val thumbnailsVttUrl: String? = null,
)

enum class ContentType { VOD, LIVE, CLIP }

/** Widevine DRM configuration. */
data class DrmConfig(
    val licenseUrl: String,
    val licenseRequestHeaders: Map<String, String> = emptyMap(),
    val multiSession: Boolean = false,
)

/** A subtitle file delivered outside the manifest (WebVTT, SRT or TTML). */
data class SubtitleTrack(
    val url: String,
    /** BCP 47 language tag, e.g. "en" or "pt-BR". */
    val language: String,
    val label: String? = null,
    /** MIME type; inferred from the URL extension when null. */
    val mimeType: String? = null,
    val isDefault: Boolean = false,
)
