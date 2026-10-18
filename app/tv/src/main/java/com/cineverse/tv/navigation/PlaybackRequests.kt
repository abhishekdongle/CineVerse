package com.cineverse.tv.navigation

import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.ContentType as PlayerContentType

/** Catalog item → player request. The app wires the two because the player knows no domain models. */
internal fun MediaItem.toPlaybackRequest() = PlaybackRequest(
    id = id,
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    streamUrl = streamUrl,
    type = when (contentType) {
        ContentType.VOD -> PlayerContentType.VOD
        ContentType.LIVE -> PlayerContentType.LIVE
        ContentType.CLIP -> PlayerContentType.CLIP
    },
    adTagUrl = adTagUrl,
)
