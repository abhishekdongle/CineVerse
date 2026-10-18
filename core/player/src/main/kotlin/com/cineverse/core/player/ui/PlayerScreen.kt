package com.cineverse.core.player.ui

import android.view.ViewGroup
import androidx.annotation.OptIn
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
import com.cineverse.core.player.CineVersePlayer
import com.cineverse.core.player.PlayableMedia

/**
 * Full-screen playback used by both apps. Media3's [PlayerView] handles touch on mobile and
 * D-pad keys on TV once it has focus.
 */
@Composable
fun PlayerScreen(
    media: PlayableMedia,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(media.id) { viewModel.load(media) }

    // Pause when the app goes to the background; resume when it comes back.
    LifecycleStartEffect(viewModel) {
        viewModel.player.play()
        onStopOrDispose { viewModel.player.pause() }
    }

    VideoSurface(
        player = viewModel.player,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}

/** The video surface plus Media3's default controls for [player]. */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(
    player: CineVersePlayer,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                    // Needed on TV so D-pad presses reach the controller.
                    isFocusable = true
                    isFocusableInTouchMode = true
                    this.player = player.exoPlayer
                    requestFocus()
                }
            },
            update = { view -> view.player = player.exoPlayer },
            onRelease = { view -> view.player = null },
        )
    }
}
