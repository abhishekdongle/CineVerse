package com.cineverse.core.data.mapper

import com.cineverse.core.domain.model.AccessModel
import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.HomeFeed
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.domain.model.Rail
import com.cineverse.core.network.model.HomeFeedDto
import com.cineverse.core.network.model.MediaItemDto
import com.cineverse.core.network.model.RailDto

internal fun HomeFeedDto.toDomain(): HomeFeed {
    val rails = rails.map(RailDto::toDomain)
    val featured = rails.asSequence().flatMap { it.items }.let { items ->
        items.firstOrNull { it.id == featuredId } ?: items.firstOrNull()
    }
    return HomeFeed(featured = featured, rails = rails)
}

internal fun RailDto.toDomain() = Rail(
    id = id,
    title = title,
    items = items.map(MediaItemDto::toDomain),
)

internal fun MediaItemDto.toDomain() = MediaItem(
    id = id,
    title = title,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl.ifEmpty { posterUrl },
    streamUrl = streamUrl,
    contentType = if (type.equals("live", ignoreCase = true)) ContentType.LIVE else ContentType.VOD,
    access = if (access.equals("avod", ignoreCase = true)) {
        AccessModel.AD_SUPPORTED
    } else {
        AccessModel.SUBSCRIPTION
    },
)
