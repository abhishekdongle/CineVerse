package com.cineverse.feature.videoplayer.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.cineverse.feature.videoplayer.ui.VideoPlayerScreen
import com.cineverse.feature.videoplayer.ui.tv.TvVideoPlayerScreen
import com.cineverse.core.player.model.ContentType
import com.cineverse.core.player.model.PlaybackRequest
import kotlinx.serialization.Serializable

/**
 * Type-safe route to the video player. Carries the fields of a [PlaybackRequest] that fit in a
 * route; DRM settings and sideloaded subtitles are not passed through navigation.
 */
@Serializable
data class VideoPlayerRoute(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val streamUrl: String,
    val type: ContentType,
    val startPositionMs: Long = 0L,
    val adTagUrl: String? = null,
) {
    fun toPlaybackRequest() = PlaybackRequest(
        id = id,
        title = title,
        subtitle = subtitle,
        posterUrl = posterUrl,
        streamUrl = streamUrl,
        type = type,
        startPositionMs = startPositionMs,
        adTagUrl = adTagUrl,
    )
}

fun NavController.navigateToVideoPlayer(request: PlaybackRequest, navOptions: NavOptions? = null) {
    navigate(
        VideoPlayerRoute(
            id = request.id,
            title = request.title,
            subtitle = request.subtitle,
            posterUrl = request.posterUrl,
            streamUrl = request.streamUrl,
            type = request.type,
            startPositionMs = request.startPositionMs,
            adTagUrl = request.adTagUrl,
        ),
        navOptions,
    )
}

/** Adds the phone/tablet player destination. */
fun NavGraphBuilder.videoPlayerScreen(onBack: () -> Unit) {
    composable<VideoPlayerRoute> { entry ->
        VideoPlayerScreen(request = entry.toRoute<VideoPlayerRoute>().toPlaybackRequest(), onBack = onBack)
    }
}

/** Adds the TV player destination. The remote's Back button pops it like any other screen. */
fun NavGraphBuilder.tvVideoPlayerScreen() {
    composable<VideoPlayerRoute> { entry ->
        TvVideoPlayerScreen(request = entry.toRoute<VideoPlayerRoute>().toPlaybackRequest())
    }
}
