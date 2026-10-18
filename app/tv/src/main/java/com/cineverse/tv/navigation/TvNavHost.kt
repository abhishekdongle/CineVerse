package com.cineverse.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cineverse.feature.home.ui.tv.HomeTvScreen
import com.cineverse.feature.videoplayer.ui.navigation.navigateToVideoPlayer
import com.cineverse.feature.videoplayer.ui.navigation.tvVideoPlayerScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** TV navigation graph. The remote's Back button pops the back stack like the system back on mobile. */
@Composable
fun TvNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeTvScreen(onPlay = { navController.navigateToVideoPlayer(it.toPlaybackRequest()) })
        }
        tvVideoPlayerScreen()
    }
}
