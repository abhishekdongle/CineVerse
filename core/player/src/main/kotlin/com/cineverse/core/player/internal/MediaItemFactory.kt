package com.cineverse.core.player.internal

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.cineverse.core.player.model.ContentType
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.SubtitleTrack

internal object MediaItemFactory {

    fun create(request: PlaybackRequest): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(request.id)
            .setUri(request.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(request.title)
                    .setSubtitle(request.subtitle)
                    // System media controls show the artist line under the title.
                    .setArtist(request.subtitle.orEmpty())
                    .setArtworkUri(request.posterUrl?.toUri())
                    .setIsPlayable(true)
                    .build(),
            )
            .setSubtitleConfigurations(request.sideloadedSubtitles.map(::subtitleConfiguration))

        manifestMimeType(request.streamUrl)?.let(builder::setMimeType)

        request.adTagUrl?.let { tag ->
            builder.setAdsConfiguration(MediaItem.AdsConfiguration.Builder(tag.toUri()).build())
        }

        request.drm?.let { drm ->
            builder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(drm.licenseUrl)
                    .setLicenseRequestHeaders(drm.licenseRequestHeaders)
                    .setMultiSession(drm.multiSession)
                    .build(),
            )
        }

        if (request.type == ContentType.LIVE) {
            // Let the player adjust speed slightly to hold a stable distance from the live edge.
            builder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder().setMaxPlaybackSpeed(1.02f).build(),
            )
        }
        return builder.build()
    }

    private fun subtitleConfiguration(track: SubtitleTrack) =
        MediaItem.SubtitleConfiguration.Builder(track.url.toUri())
            .setMimeType(track.mimeType ?: subtitleMimeType(track.url))
            .setLanguage(track.language)
            .setLabel(track.label)
            .setSelectionFlags(if (track.isDefault) C.SELECTION_FLAG_DEFAULT else 0)
            .build()

    private fun manifestMimeType(url: String): String? {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
            path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
            else -> null
        }
    }

    private fun subtitleMimeType(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            path.endsWith(".ttml") || path.endsWith(".xml") || path.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.TEXT_VTT
        }
    }
}
