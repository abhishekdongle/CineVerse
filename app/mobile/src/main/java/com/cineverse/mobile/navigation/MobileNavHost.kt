package com.cineverse.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cineverse.core.domain.model.ContentType
import com.cineverse.core.domain.model.MediaItem
import com.cineverse.core.player.PlayableMedia
import com.cineverse.core.player.ui.PlayerScreen
import com.cineverse.feature.home.ui.mobile.HomeMobileScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class PlayerRoute(
    val id: String,
    val title: String,
    val streamUrl: String,
    val isLive: Boolean,
)

/** Mobile navigation graph. Navigation is platform code, so each app owns its own graph. */
@Composable
fun MobileNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeMobileScreen(onPlay = { navController.navigate(it.toPlayerRoute()) })
        }
        composable<PlayerRoute> { entry ->
            val route = entry.toRoute<PlayerRoute>()
            PlayerScreen(PlayableMedia(route.id, route.title, route.streamUrl, route.isLive))
        }
    }
}

private fun MediaItem.toPlayerRoute() = PlayerRoute(
    id = id,
    title = title,
    streamUrl = streamUrl,
    isLive = contentType == ContentType.LIVE,
)
