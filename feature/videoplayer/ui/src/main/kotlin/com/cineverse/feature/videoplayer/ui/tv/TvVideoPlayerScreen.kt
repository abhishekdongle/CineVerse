@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.cineverse.feature.videoplayer.ui.tv

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
import com.cineverse.feature.videoplayer.presentation.VideoPlayerAction
import com.cineverse.feature.videoplayer.presentation.VideoPlayerViewModel
import com.cineverse.core.player.model.PlaybackRequest

/**
 * Interim TV player: the same VideoPlayerViewModel and player as mobile, with Media3's
 * D-pad-ready PlayerView controls. Custom Compose for TV controls driven by
 * VideoPlayerUiState can replace this without changing :core:player or the ViewModel.
 */
@Composable
fun TvVideoPlayerScreen(request: PlaybackRequest, modifier: Modifier = Modifier) {
    val viewModel: VideoPlayerViewModel = hiltViewModel()
    LaunchedEffect(viewModel, request.id) { viewModel.onAction(VideoPlayerAction.Load(request)) }
    LifecycleStartEffect(viewModel) {
        onStopOrDispose { viewModel.onAction(VideoPlayerAction.AppBackgrounded) }
    }
    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                    isFocusable = true
                    player = viewModel.player
                    requestFocus()
                }
            },
            onRelease = { it.player = null },
        )
        // IMA ad UI for AVOD titles.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                (viewModel.adViewContainer.parent as? ViewGroup)?.removeView(viewModel.adViewContainer)
                viewModel.adViewContainer
            },
            onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        )
    }
}
