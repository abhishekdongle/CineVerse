@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.cineverse.feature.videoplayer.ui.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.cineverse.feature.videoplayer.ui.R

/** Video surface, or a placeholder while casting. Letterboxed to the video's aspect ratio. */
@Composable
internal fun VideoSurface(
    player: Player,
    isCasting: Boolean,
    castDeviceName: String?,
    background: Color,
    useTextureView: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(background)) {
        if (isCasting) {
            Text(
                text = castDeviceName?.let { stringResource(R.string.player_casting_to, it) }
                    ?: stringResource(R.string.player_casting),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            ContentFrame(
                player,
                Modifier.fillMaxSize(),
                if (useTextureView) SURFACE_TYPE_TEXTURE_VIEW else SURFACE_TYPE_SURFACE_VIEW,
                ContentScale.Fit,
                false,
            ) {
                Box(Modifier.fillMaxSize().background(background))
            }
        }
    }
}

/**
 * Subtitles rendered by Media3's SubtitleView, which applies the user's system caption style and
 * text size (Settings > Accessibility > Caption preferences).
 */
@Composable
internal fun SubtitleLayer(
    player: Player,
    bottomPaddingFraction: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = remember(context) {
        SubtitleView(context).apply {
            setUserDefaultStyle()
            setUserDefaultTextSize()
        }
    }
    DisposableEffect(player, view) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                view.setCues(cueGroup.cues)
            }
        }
        view.setCues(player.currentCues.cues)
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            view.setCues(null)
        }
    }
    AndroidView(
        factory = { view },
        update = { it.setBottomPaddingFraction(bottomPaddingFraction) },
        modifier = modifier,
    )
}

/**
 * Hosts the controller-owned IMA ad container. The same view is moved into the new hierarchy
 * after a configuration change, so an ad in progress keeps its UI.
 */
@Composable
internal fun AdContainer(container: ViewGroup, modifier: Modifier = Modifier) {
    AndroidView(
        factory = {
            (container.parent as? ViewGroup)?.removeView(container)
            container.apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        modifier = modifier.fillMaxSize(),
    )
}
