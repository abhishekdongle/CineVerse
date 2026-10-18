package com.cineverse.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeFeedDto(
    @SerialName("featured_id") val featuredId: String? = null,
    val rails: List<RailDto> = emptyList(),
)

@Serializable
data class RailDto(
    val id: String,
    val title: String,
    val items: List<MediaItemDto> = emptyList(),
)

@Serializable
data class MediaItemDto(
    val id: String,
    val title: String,
    val description: String = "",
    @SerialName("poster_url") val posterUrl: String = "",
    @SerialName("backdrop_url") val backdropUrl: String = "",
    @SerialName("stream_url") val streamUrl: String,
    /** "vod" or "live". */
    val type: String,
    /** "svod" or "avod". */
    val access: String,
)
