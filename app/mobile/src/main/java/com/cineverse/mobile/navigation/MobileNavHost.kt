package com.cineverse.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.ui.mobile.PlayerScreen
import com.cineverse.feature.home.ui.mobile.HomeMobileScreen
import kotlinx.serialization.Serializable
import com.cineverse.core.player.model.ContentType as PlayerContentType

@Serializable
data object HomeRoute

/** Flat, serializable copy of the fields needed to build a [PlaybackRequest]. */
@Serializable
data class PlayerRoute(
    val id: String,
    val title: String,
    val subtitle: String?,
    val posterUrl: String?,
    val streamUrl: String,
    val type: String,
    val adTagUrl: String?,
)

/** Mobile navigation graph. Navigation is platform code, so each app owns its own graph. */
@Composable
fun MobileNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Surface(modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                Box(Modifier.safeDrawingPadding()) {
                    HomeMobileScreen(onPlay = { navController.navigate(it.toPlayerRoute()) })
                }
            }
            composable<PlayerRoute> { entry ->
                PlayerScreen(
                    request = entry.toRoute<PlayerRoute>().toPlaybackRequest(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun MediaItem.toPlayerRoute() = PlayerRoute(
    id = id,
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    streamUrl = streamUrl,
    type = contentType.name,
    adTagUrl = adTagUrl,
)

private fun PlayerRoute.toPlaybackRequest() = PlaybackRequest(
    id = id,
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    streamUrl = streamUrl,
    type = when (ContentType.valueOf(type)) {
        ContentType.VOD -> PlayerContentType.VOD
        ContentType.LIVE -> PlayerContentType.LIVE
        ContentType.CLIP -> PlayerContentType.CLIP
    },
    adTagUrl = adTagUrl,
)
