package com.cineverse.core.player.ui.mobile

import android.content.res.Configuration
import android.graphics.Rect
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import androidx.window.core.layout.WindowSizeClass
import com.cineverse.core.player.PlayerController
import com.cineverse.core.player.PlayerViewModel
import com.cineverse.core.player.cast.CastSupport
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlaybackStatus
import com.cineverse.core.player.model.PlayerState
import com.cineverse.core.player.ui.mobile.controls.ControlActions
import com.cineverse.core.player.ui.mobile.controls.ErrorOverlay
import com.cineverse.core.player.ui.mobile.controls.PlayerControls
import com.cineverse.core.player.ui.mobile.controls.SeekFeedback
import com.cineverse.core.player.ui.mobile.settings.SettingsPanel
import com.cineverse.core.player.ui.mobile.system.ImmersiveModeEffect
import com.cineverse.core.player.ui.mobile.system.PictureInPictureEffect
import com.cineverse.core.player.ui.mobile.system.findActivity
import com.cineverse.core.player.ui.mobile.system.rememberFullscreenOrientation
import com.cineverse.core.player.ui.mobile.system.rememberIsInPictureInPicture
import com.cineverse.core.player.ui.mobile.system.rememberTouchExplorationEnabled
import kotlinx.coroutines.delay

/**
 * Full player screen for phones, tablets and foldables. Starts playing [request] automatically.
 *
 * The player lives in a [PlayerViewModel] scoped to the current navigation entry (or activity),
 * so rotation, fullscreen, resizing and PiP never recreate it.
 *
 * Host requirements: the activity is a ComponentActivity using Hilt (`@AndroidEntryPoint`) and,
 * for PiP, declares `android:supportsPictureInPicture="true"` and handles
 * `screenSize|smallestScreenSize|screenLayout|orientation` config changes.
 */
@Composable
fun PlayerScreen(
    request: PlaybackRequest,
    config: PlayerUiConfig = PlayerUiConfig(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlayerViewModel = hiltViewModel()
    PlayerScreen(viewModel.controller, request, config, onBack, modifier)
}

/** Same as the other overload, for hosts that create and own the [PlayerController] themselves. */
@Composable
fun PlayerScreen(
    controller: PlayerController,
    request: PlaybackRequest,
    config: PlayerUiConfig = PlayerUiConfig(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(controller, request.id) { controller.load(request) }

    val state by controller.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val icons = config.icons ?: PlayerUiDefaults.icons()
    val accent = config.colors.accent.takeIf { it.isSpecified } ?: MaterialTheme.colorScheme.primary

    // Layout class: tablets/unfolded foldables never auto-rotate or auto-fullscreen.
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isLargeScreen = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isInPip = rememberIsInPictureInPicture(activity)
    var userFullscreen by rememberSaveable { mutableStateOf(false) }
    val isFullscreen = userFullscreen || (!isLargeScreen && isLandscape)
    val orientation = rememberFullscreenOrientation(activity)
    ImmersiveModeEffect(activity, immersive = isFullscreen && !isInPip)

    val toggleFullscreen = {
        if (isFullscreen) {
            userFullscreen = false
            if (!isLargeScreen) orientation?.exit(isLandscapeNow = isLandscape)
        } else {
            userFullscreen = true
            val landscapeVideo = state.videoWidth == 0 || state.videoWidth >= state.videoHeight
            if (!isLargeScreen) orientation?.enter(rotateToLandscape = landscapeVideo && !isLandscape)
        }
    }
    BackHandler(enabled = isFullscreen && !isInPip) { toggleFullscreen() }

    // Pause when the app goes to the background, unless casting (the TV keeps playing).
    // PiP keeps the activity started, so this doesn't fire while in PiP.
    LifecycleStartEffect(controller) {
        onStopOrDispose { if (!controller.state.value.isCasting) controller.pause() }
    }

    var sourceRectHint by remember { mutableStateOf<Rect?>(null) }
    PictureInPictureEffect(
        activity = activity,
        enabled = config.enablePictureInPicture && !state.isCasting,
        isPlaying = state.isPlaying,
        videoWidth = state.videoWidth,
        videoHeight = state.videoHeight,
        sourceRectHint = sourceRectHint,
        onPlay = controller::play,
        onPause = controller::pause,
    )

    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val castAvailable = remember(context) { CastSupport.isAvailable(context) }

    Box(
        modifier
            .fillMaxSize()
            .background(if (isFullscreen || isInPip) config.colors.videoBackground else MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            // The video box keeps the same position in the tree in every mode, so the surface
            // and ad container are never recreated when switching inline/fullscreen.
            PlayerSurfaceArea(
                controller = controller,
                state = state,
                request = request,
                config = config,
                icons = icons,
                accent = accent,
                isFullscreen = isFullscreen,
                isInPip = isInPip,
                large = isLargeScreen,
                showCastButton = castAvailable && config.controls.showCast,
                settingsOpen = settingsOpen,
                onOpenSettings = { settingsOpen = true },
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
                    request,
                    state,
                    Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = if (isLargeScreen) 960.dp else 720.dp),
                )
            }
        }
        if (!isInPip) {
            SettingsPanel(
                visible = settingsOpen && !state.isPlayingAd,
                asSidePanel = isLandscape || isLargeScreen,
                state = state,
                controller = controller,
                controls = config.controls,
                accent = accent,
                onDismiss = { settingsOpen = false },
            )
        }
    }
}

@Composable
private fun PlayerSurfaceArea(
    controller: PlayerController,
    state: PlayerState,
    request: PlaybackRequest,
    config: PlayerUiConfig,
    icons: PlayerIcons,
    accent: androidx.compose.ui.graphics.Color,
    isFullscreen: Boolean,
    isInPip: Boolean,
    large: Boolean,
    showCastButton: Boolean,
    settingsOpen: Boolean,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var interactions by remember { mutableIntStateOf(0) }
    var scrubPositionMs by remember { mutableStateOf<Long?>(null) }
    var seekFeedback by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
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

    // Auto-hide: only while playing, never while TalkBack is on, settings are open or scrubbing.
    LaunchedEffect(controlsVisible, interactions, state.isPlaying, settingsOpen, scrubPositionMs, touchExploration) {
        if (controlsVisible && state.isPlaying && !settingsOpen && scrubPositionMs == null && !touchExploration) {
            delay(hideDelay)
            controlsVisible = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(700)
            seekFeedback = null
        }
    }
    val showControls = controlsVisible || touchExploration || state.status == PlaybackStatus.Ended

    fun interacted() {
        interactions++
    }

    val seekSeconds = (config.seekIncrementMs / 1000).toInt()
    val actions = ControlActions(
        onBack = onBack,
        onTogglePlayPause = { interacted(); controller.togglePlayPause() },
        onSeekBack = { interacted(); controller.seekBack(config.seekIncrementMs) },
        onSeekForward = { interacted(); controller.seekForward(config.seekIncrementMs) },
        onSeek = { interacted(); controller.seekTo(it) },
        onScrub = { scrubPositionMs = it },
        onGoToLive = { interacted(); controller.goToLive() },
        onToggleFullscreen = { interacted(); onToggleFullscreen() },
        onOpenSettings = { interacted(); onOpenSettings() },
    )
    val slotScope = object : PlayerSlotScope {
        override val state = state
        override val controller = controller
        override val controlsVisible = showControls
        override val isFullscreen = isFullscreen
    }

    Box(
        modifier
            .background(config.colors.videoBackground)
            .then(if (isFullscreen && !isInPip) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier),
    ) {
        com.cineverse.core.player.ui.mobile.video.VideoSurface(
            player = controller.player,
            state = state,
            background = config.colors.videoBackground,
            useTextureView = config.useTextureView,
            modifier = Modifier.fillMaxSize(),
        )
        com.cineverse.core.player.ui.mobile.video.SubtitleLayer(
            player = controller.player,
            bottomPaddingFraction = if (showControls && !isInPip) 0.34f else 0.08f,
            modifier = Modifier.fillMaxSize(),
        )
        // Below the gesture/controls layers, which are removed during ads so IMA's UI gets touches.
        com.cineverse.core.player.ui.mobile.video.AdContainer(controller.adViewContainer)

        if (!isInPip && !state.isPlayingAd) {
            config.slots.overlay?.invoke(slotScope)

            // Tap toggles controls; double-tap left/right seeks. Hidden from TalkBack, which
            // keeps the controls visible instead.
            Box(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {}
                    .pointerInput(config.enableDoubleTapToSeek, state.canSeek) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interacted()
                            },
                            onDoubleTap = if (config.enableDoubleTapToSeek && state.canSeek) {
                                { offset: Offset ->
                                    val forward = offset.x > size.width / 2
                                    if (forward) controller.seekForward(config.seekIncrementMs) else controller.seekBack(config.seekIncrementMs)
                                    val previous = seekFeedback?.takeIf { it.first == forward }?.second ?: 0
                                    seekFeedback = forward to previous + seekSeconds
                                }
                            } else {
                                null
                            },
                        )
                    },
            )

            seekFeedback?.let { (forward, seconds) -> SeekFeedback(forward, seconds) }

            AnimatedVisibility(showControls, enter = fadeIn(), exit = fadeOut()) {
                PlayerControls(
                    state = state,
                    scrubPositionMs = scrubPositionMs,
                    title = request.title,
                    subtitle = request.subtitle,
                    isFullscreen = isFullscreen,
                    showCastButton = showCastButton,
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

            state.error?.let { error ->
                ErrorOverlay(
                    message = error.message ?: error.codeName,
                    icons = icons,
                    colors = config.colors,
                    onRetry = controller::retry,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun InlineDetails(request: PlaybackRequest, state: PlayerState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Text(request.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        request.subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (state.isLive) {
            Text(
                stringResource(R.string.player_live),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
