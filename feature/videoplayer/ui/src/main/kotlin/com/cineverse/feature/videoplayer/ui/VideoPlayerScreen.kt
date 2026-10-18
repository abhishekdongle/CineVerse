package com.cineverse.feature.videoplayer.ui

import android.content.res.Configuration
import android.graphics.Rect
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.window.core.layout.WindowSizeClass
import com.cineverse.feature.videoplayer.presentation.VideoPlayerAction
import com.cineverse.feature.videoplayer.presentation.VideoPlayerUiState
import com.cineverse.feature.videoplayer.presentation.VideoPlayerViewModel
import com.cineverse.feature.videoplayer.ui.controls.ControlActions
import com.cineverse.feature.videoplayer.ui.controls.ErrorOverlay
import com.cineverse.feature.videoplayer.ui.controls.PlayerControls
import com.cineverse.feature.videoplayer.ui.controls.SeekFeedbackBubble
import com.cineverse.feature.videoplayer.ui.settings.SettingsPanel
import com.cineverse.feature.videoplayer.ui.system.ImmersiveModeEffect
import com.cineverse.feature.videoplayer.ui.system.PictureInPictureEffect
import com.cineverse.feature.videoplayer.ui.system.PictureInPictureModeEffect
import com.cineverse.feature.videoplayer.ui.system.findActivity
import com.cineverse.feature.videoplayer.ui.system.rememberFullscreenOrientation
import com.cineverse.feature.videoplayer.ui.system.rememberTouchExplorationEnabled
import com.cineverse.feature.videoplayer.ui.video.AdContainer
import com.cineverse.feature.videoplayer.ui.video.SubtitleLayer
import com.cineverse.feature.videoplayer.ui.video.VideoSurface
import com.cineverse.core.player.model.PlaybackRequest

/**
 * Full video player screen for phones, tablets and foldables. Starts playing [request]
 * automatically.
 *
 * The player lives in a [VideoPlayerViewModel] scoped to the current navigation entry (or
 * activity), so rotation, fullscreen, resizing and PiP never recreate it.
 *
 * Host requirements: the activity is a ComponentActivity using Hilt (`@AndroidEntryPoint`) and,
 * for PiP, declares `android:supportsPictureInPicture="true"` and handles
 * `screenSize|smallestScreenSize|screenLayout|orientation` config changes.
 */
@Composable
fun VideoPlayerScreen(
    request: PlaybackRequest,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    config: PlayerUiConfig = PlayerUiConfig(),
) {
    val viewModel: VideoPlayerViewModel = hiltViewModel()
    LaunchedEffect(viewModel, request.id) { viewModel.onAction(VideoPlayerAction.Load(request)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideoPlayerContent(
        uiState = uiState,
        player = viewModel.player,
        adViewContainer = viewModel.adViewContainer,
        config = config,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Renders [uiState] and reports user input and window changes through [onAction]. Window work
 * that needs an Activity (orientation, immersive mode, PiP) happens here.
 */
@Composable
internal fun VideoPlayerContent(
    uiState: VideoPlayerUiState,
    player: Player,
    adViewContainer: ViewGroup,
    config: PlayerUiConfig,
    onAction: (VideoPlayerAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val icons = config.icons ?: PlayerUiDefaults.icons()
    val accent = config.colors.accent.takeIf { it.isSpecified } ?: MaterialTheme.colorScheme.primary

    // Layout class: tablets/unfolded foldables never auto-rotate or auto-fullscreen.
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isLargeScreen = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isLargeScreen, isLandscape) {
        onAction(VideoPlayerAction.WindowChanged(isLargeScreen, isLandscape))
    }
    PictureInPictureModeEffect(activity) { onAction(VideoPlayerAction.PictureInPictureChanged(it)) }

    val touchExploration = rememberTouchExplorationEnabled()
    val accessibilityManager = LocalAccessibilityManager.current
    val hideDelay = remember(accessibilityManager, config.autoHideDelayMs) {
        accessibilityManager?.calculateRecommendedTimeoutMillis(
            config.autoHideDelayMs,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: config.autoHideDelayMs
    }
    LaunchedEffect(touchExploration, hideDelay) {
        onAction(VideoPlayerAction.AccessibilityChanged(touchExploration, hideDelay))
    }

    val isInPip = uiState.isInPictureInPicture
    val isFullscreen = uiState.isFullscreen
    val orientation = rememberFullscreenOrientation(activity)
    ImmersiveModeEffect(activity, immersive = isFullscreen && !isInPip)

    val toggleFullscreen = {
        if (!isLargeScreen) {
            if (isFullscreen) {
                orientation?.exit(isLandscapeNow = isLandscape)
            } else {
                orientation?.enter(rotateToLandscape = uiState.isLandscapeVideo && !isLandscape)
            }
        }
        onAction(VideoPlayerAction.FullscreenToggled)
    }
    BackHandler(enabled = isFullscreen && !isInPip) { toggleFullscreen() }

    // PiP keeps the activity started, so this doesn't fire while in PiP.
    LifecycleStartEffect(Unit) {
        onStopOrDispose { onAction(VideoPlayerAction.AppBackgrounded) }
    }

    var sourceRectHint by remember { mutableStateOf<Rect?>(null) }
    PictureInPictureEffect(
        activity = activity,
        enabled = config.enablePictureInPicture && uiState.canEnterPictureInPicture,
        isPlaying = uiState.isPlaying,
        videoWidth = uiState.videoWidth,
        videoHeight = uiState.videoHeight,
        sourceRectHint = sourceRectHint,
        onPlay = { onAction(VideoPlayerAction.PlayClicked) },
        onPause = { onAction(VideoPlayerAction.PauseClicked) },
    )

    Box(
        modifier
            .fillMaxSize()
            .background(if (isFullscreen || isInPip) config.colors.videoBackground else MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            // The video box keeps the same position in the tree in every mode, so the surface
            // and ad container are never recreated when switching inline/fullscreen.
            PlayerSurfaceArea(
                uiState = uiState,
                player = player,
                adViewContainer = adViewContainer,
                config = config,
                icons = icons,
                accent = accent,
                large = isLargeScreen,
                onAction = onAction,
                onToggleFullscreen = toggleFullscreen,
                onBack = onBack,
                modifier = if (isFullscreen || isInPip) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .statusBarsPadding()
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = if (isLargeScreen) 960.dp else 720.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }.onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    sourceRectHint = Rect(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
                },
            )
            if (!isFullscreen && !isInPip) {
                InlineDetails(
                    uiState,
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = if (isLargeScreen) 960.dp else 720.dp),
                )
            }
        }
        if (!isInPip) {
            SettingsPanel(
                settings = uiState.settings,
                controls = config.controls,
                accent = accent,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PlayerSurfaceArea(
    uiState: VideoPlayerUiState,
    player: Player,
    adViewContainer: ViewGroup,
    config: PlayerUiConfig,
    icons: PlayerIcons,
    accent: Color,
    large: Boolean,
    onAction: (VideoPlayerAction) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFullscreen = uiState.isFullscreen
    val isInPip = uiState.isInPictureInPicture
    val showControls = uiState.controlsVisible
    val seekSeconds = (config.seekIncrementMs / 1000).toInt()
    val actions = ControlActions(
        onBack = onBack,
        onTogglePlayPause = { onAction(VideoPlayerAction.PlayPauseClicked) },
        onSeekBack = { onAction(VideoPlayerAction.SeekBackClicked(config.seekIncrementMs)) },
        onSeekForward = { onAction(VideoPlayerAction.SeekForwardClicked(config.seekIncrementMs)) },
        onSeek = { onAction(VideoPlayerAction.SeekTo(it)) },
        onScrub = { onAction(VideoPlayerAction.Scrubbing(it)) },
        onGoToLive = { onAction(VideoPlayerAction.GoToLiveClicked) },
        onToggleFullscreen = onToggleFullscreen,
        onOpenSettings = { onAction(VideoPlayerAction.SettingsOpened) },
    )
    val slotScope = object : PlayerSlotScope {
        override val uiState = uiState
        override val controlsVisible = showControls
        override val isFullscreen = isFullscreen
        override val onAction = onAction
    }

    Box(
        modifier
            .background(config.colors.videoBackground)
            .then(if (isFullscreen && !isInPip) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier),
    ) {
        VideoSurface(
            player = player,
            isCasting = uiState.isCasting,
            castDeviceName = uiState.castDeviceName,
            background = config.colors.videoBackground,
            useTextureView = config.useTextureView,
            modifier = Modifier.fillMaxSize(),
        )
        SubtitleLayer(
            player = player,
            bottomPaddingFraction = if (showControls && !isInPip) 0.34f else 0.08f,
            modifier = Modifier.fillMaxSize(),
        )
        // Below the gesture/controls layers, which are removed during ads so IMA's UI gets touches.
        AdContainer(adViewContainer)

        if (!isInPip && !uiState.isPlayingAd) {
            config.slots.overlay?.invoke(slotScope)

            // Tap toggles controls; double-tap left/right seeks. Hidden from TalkBack, which
            // keeps the controls visible instead.
            Box(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {}
                    .pointerInput(config.enableDoubleTapToSeek, config.seekIncrementMs, uiState.canSeek) {
                        detectTapGestures(
                            onTap = { onAction(VideoPlayerAction.VideoTapped) },
                            onDoubleTap = if (config.enableDoubleTapToSeek && uiState.canSeek) {
                                { offset: Offset ->
                                    onAction(VideoPlayerAction.DoubleTapped(offset.x > size.width / 2, config.seekIncrementMs))
                                }
                            } else {
                                null
                            },
                        )
                    },
            )

            uiState.seekFeedback?.let { SeekFeedbackBubble(it.forward, it.seconds) }

            AnimatedVisibility(showControls, enter = fadeIn(), exit = fadeOut()) {
                PlayerControls(
                    uiState = uiState,
                    isFullscreen = isFullscreen,
                    showCastButton = uiState.showCastButton && config.controls.showCast,
                    large = large,
                    seekIncrementSeconds = seekSeconds,
                    icons = icons,
                    colors = config.colors,
                    accent = accent,
                    controls = config.controls,
                    slots = config.slots,
                    slotScope = slotScope,
                    actions = actions,
                )
            }

            uiState.error?.let { error ->
                ErrorOverlay(
                    message = error.message,
                    icons = icons,
                    colors = config.colors,
                    onRetry = { onAction(VideoPlayerAction.RetryClicked) },
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun InlineDetails(uiState: VideoPlayerUiState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Text(uiState.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        uiState.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (uiState.isLive) {
            Text(
                stringResource(R.string.player_live),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
