package com.cineverse.feature.videoplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cineverse.feature.videoplayer.presentation.VideoPlayerAction
import com.cineverse.feature.videoplayer.presentation.VideoPlayerUiState
import com.cineverse.core.player.PlayerController

/** Everything the host app can customize on [VideoPlayerScreen]. All parameters have defaults. */
@Immutable
data class PlayerUiConfig(
    val colors: PlayerColors = PlayerColors(),
    /** Null uses the built-in icons ([PlayerUiDefaults.icons]). */
    val icons: PlayerIcons? = null,
    val controls: PlayerControlsConfig = PlayerControlsConfig(),
    val seekIncrementMs: Long = PlayerController.DEFAULT_SEEK_INCREMENT_MS,
    /** Controls hide after this long without interaction. Accessibility timeouts can extend it. */
    val autoHideDelayMs: Long = 4_000L,
    val slots: PlayerSlots = PlayerSlots(),
    val enablePictureInPicture: Boolean = true,
    val enableDoubleTapToSeek: Boolean = true,
    /**
     * Render into a TextureView instead of a SurfaceView. SurfaceView (the default) is more
     * power-efficient and required for secure (Widevine L1) output; use TextureView only for
     * devices whose compositor mis-renders SurfaceView in PiP or during animations.
     */
    val useTextureView: Boolean = false,
)

@Immutable
data class PlayerColors(
    /** Seek bar progress and selected settings. [Color.Unspecified] uses `MaterialTheme.colorScheme.primary`. */
    val accent: Color = Color.Unspecified,
    /** Icons and text drawn over video. */
    val content: Color = Color.White,
    val scrim: Color = Color.Black.copy(alpha = 0.5f),
    val bufferedTrack: Color = Color.White.copy(alpha = 0.5f),
    val inactiveTrack: Color = Color.White.copy(alpha = 0.24f),
    val liveBadge: Color = Color(0xFFD32F2F),
    val videoBackground: Color = Color.Black,
)

@Immutable
data class PlayerIcons(
    val play: ImageVector,
    val pause: ImageVector,
    val seekBack: ImageVector,
    val seekForward: ImageVector,
    val enterFullscreen: ImageVector,
    val exitFullscreen: ImageVector,
    val settings: ImageVector,
    val back: ImageVector,
    val retry: ImageVector,
)

/** Which built-in controls are shown. */
@Immutable
data class PlayerControlsConfig(
    val showBack: Boolean = true,
    val showTitle: Boolean = true,
    val showSeekButtons: Boolean = true,
    val showFullscreen: Boolean = true,
    val showCast: Boolean = true,
    val showSettings: Boolean = true,
    val showQuality: Boolean = true,
    val showAudio: Boolean = true,
    val showSubtitles: Boolean = true,
    val showSpeed: Boolean = true,
)

/**
 * App-provided content drawn over the video. [topBarActions], [center] and [bottomBar] show and
 * hide with the controls; [overlay] is always visible (watermark, "Skip intro", live chat).
 * Nothing is drawn in Picture-in-Picture or during ads.
 */
@Immutable
data class PlayerSlots(
    val topBarActions: (@Composable PlayerSlotScope.() -> Unit)? = null,
    val center: (@Composable PlayerSlotScope.() -> Unit)? = null,
    val bottomBar: (@Composable PlayerSlotScope.() -> Unit)? = null,
    val overlay: (@Composable PlayerSlotScope.() -> Unit)? = null,
)

/** What slot content can read, and how it controls the player (e.g. `onAction(SeekTo(30_000))`). */
interface PlayerSlotScope {
    val uiState: VideoPlayerUiState
    val controlsVisible: Boolean
    val isFullscreen: Boolean
    val onAction: (VideoPlayerAction) -> Unit
}

object PlayerUiDefaults {
    @Composable
    fun icons(): PlayerIcons = PlayerIcons(
        play = ImageVector.vectorResource(R.drawable.ic_player_play),
        pause = ImageVector.vectorResource(R.drawable.ic_player_pause),
        seekBack = ImageVector.vectorResource(R.drawable.ic_player_seek_back),
        seekForward = ImageVector.vectorResource(R.drawable.ic_player_seek_forward),
        enterFullscreen = ImageVector.vectorResource(R.drawable.ic_player_fullscreen),
        exitFullscreen = ImageVector.vectorResource(R.drawable.ic_player_fullscreen_exit),
        settings = ImageVector.vectorResource(R.drawable.ic_player_settings),
        back = ImageVector.vectorResource(R.drawable.ic_player_back),
        retry = ImageVector.vectorResource(R.drawable.ic_player_retry),
    )
}
