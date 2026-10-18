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
import com.cineverse.feature.home.ui.mobile.HomeMobileScreen
import com.cineverse.feature.videoplayer.ui.navigation.navigateToVideoPlayer
import com.cineverse.feature.videoplayer.ui.navigation.videoPlayerScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

/** Mobile navigation graph. Navigation is platform code, so each app owns its own graph. */
@Composable
fun MobileNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Surface(modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                Box(Modifier.safeDrawingPadding()) {
                    HomeMobileScreen(onPlay = { navController.navigateToVideoPlayer(it.toPlaybackRequest()) })
                }
            }
            videoPlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
