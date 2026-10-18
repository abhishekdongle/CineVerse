@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.cineverse.tv.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.cineverse.core.player.PlayerViewModel
import com.cineverse.core.player.model.PlaybackRequest

/**
 * Interim TV player: the same PlayerViewModel/PlayerController as mobile, with Media3's
 * D-pad-ready PlayerView controls. A :core:player-ui-tv module with custom Compose for TV
 * controls would replace this without changing :core:player.
 */
@Composable
fun TvPlayerScreen(request: PlaybackRequest, viewModel: PlayerViewModel = hiltViewModel()) {
    val controller = viewModel.controller
    LaunchedEffect(request.id) { controller.load(request) }
    LifecycleStartEffect(controller) {
        onStopOrDispose { controller.pause() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                    isFocusable = true
                    player = controller.player
                    requestFocus()
                }
            },
            onRelease = { it.player = null },
        )
        // IMA ad UI for AVOD titles.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (controller.adViewContainer.parent as? ViewGroup)?.removeView(controller.adViewContainer)
                controller.adViewContainer
            },
            onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        )
    }
}
