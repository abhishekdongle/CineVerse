package com.cineverse.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.tv.player.TvPlayerScreen
import com.cineverse.feature.home.ui.tv.HomeTvScreen
import kotlinx.serialization.Serializable
import com.cineverse.core.player.model.ContentType as PlayerContentType

@Serializable
data object HomeRoute

@Serializable
data class PlayerRoute(
    val id: String,
    val title: String,
    val streamUrl: String,
    val type: String,
    val adTagUrl: String?,
)

/** TV navigation graph. The remote's Back button pops the back stack like the system back on mobile. */
@Composable
fun TvNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeTvScreen(onPlay = { navController.navigate(it.toPlayerRoute()) })
        }
        composable<PlayerRoute> { entry ->
            val route = entry.toRoute<PlayerRoute>()
            TvPlayerScreen(
                PlaybackRequest(
                    id = route.id,
                    title = route.title,
                    streamUrl = route.streamUrl,
                    type = PlayerContentType.valueOf(route.type),
                    adTagUrl = route.adTagUrl,
                ),
            )
        }
    }
}

private fun MediaItem.toPlayerRoute() = PlayerRoute(
    id = id,
    title = title,
    streamUrl = streamUrl,
    type = when (contentType) {
        ContentType.VOD -> PlayerContentType.VOD
        ContentType.LIVE -> PlayerContentType.LIVE
        ContentType.CLIP -> PlayerContentType.CLIP
    }.name,
    adTagUrl = adTagUrl,
)
