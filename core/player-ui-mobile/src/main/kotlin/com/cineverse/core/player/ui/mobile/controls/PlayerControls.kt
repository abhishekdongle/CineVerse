package com.cineverse.core.player.ui.mobile.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.cast.MediaRouteButton
import com.cineverse.core.player.model.PlayerState
import com.cineverse.core.player.ui.mobile.PlayerColors
import com.cineverse.core.player.ui.mobile.PlayerControlsConfig
import com.cineverse.core.player.ui.mobile.PlayerIcons
import com.cineverse.core.player.ui.mobile.PlayerSlotScope
import com.cineverse.core.player.ui.mobile.PlayerSlots
import com.cineverse.core.player.ui.mobile.R

/** User intents from the controls. [onInteraction] restarts the auto-hide timer. */
internal class ControlActions(
    val onBack: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onSeekBack: () -> Unit,
    val onSeekForward: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onScrub: (Long?) -> Unit,
    val onGoToLive: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * The control overlay. Traversal order (TalkBack, keyboard, D-pad) runs top bar → center →
 * bottom bar, matching the visual order.
 */
@Composable
internal fun PlayerControls(
    state: PlayerState,
    scrubPositionMs: Long?,
    title: String,
    subtitle: String?,
    isFullscreen: Boolean,
    showCastButton: Boolean,
    large: Boolean,
    seekIncrementSeconds: Int,
    icons: PlayerIcons,
    colors: PlayerColors,
    accent: Color,
    controls: PlayerControlsConfig,
    slots: PlayerSlots,
    slotScope: PlayerSlotScope,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Box(modifier.fillMaxSize().background(colors.scrim)) {
            TopBar(
                title = title,
                subtitle = subtitle,
                showCastButton = showCastButton,
                icons = icons,
                controls = controls,
                slots = slots,
                slotScope = slotScope,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .semantics { isTraversalGroup = true; traversalIndex = 0f },
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { isTraversalGroup = true; traversalIndex = 1f },
            ) {
                CenterControls(state, large, seekIncrementSeconds, icons, controls, actions)
                slots.center?.invoke(slotScope)
            }
            BottomBar(
                state = state,
                scrubPositionMs = scrubPositionMs,
                isFullscreen = isFullscreen,
                icons = icons,
                colors = colors,
                accent = accent,
                controls = controls,
                slots = slots,
                slotScope = slotScope,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .semantics { isTraversalGroup = true; traversalIndex = 2f },
            )
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    subtitle: String?,
    showCastButton: Boolean,
    icons: PlayerIcons,
    controls: PlayerControlsConfig,
    slots: PlayerSlots,
    slotScope: PlayerSlotScope,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        if (controls.showBack) {
            PlayerIconButton(icons.back, stringResource(R.string.player_back), actions.onBack)
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            if (controls.showTitle) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        slots.topBarActions?.invoke(slotScope)
        if (showCastButton) {
            MediaRouteButton(Modifier.size(48.dp))
        }
        if (controls.showSettings) {
            PlayerIconButton(icons.settings, stringResource(R.string.player_settings), actions.onOpenSettings)
        }
    }
}

@Composable
private fun CenterControls(
    state: PlayerState,
    large: Boolean,
    seekIncrementSeconds: Int,
    icons: PlayerIcons,
    controls: PlayerControlsConfig,
    actions: ControlActions,
) {
    val mainSize: Dp = if (large) 88.dp else 64.dp
    val seekSize: Dp = if (large) 64.dp else 52.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (large) 48.dp else 28.dp),
    ) {
        if (controls.showSeekButtons && state.canSeek) {
            SeekButton(
                icon = icons.seekBack,
                seconds = seekIncrementSeconds,
                description = stringResource(R.string.player_seek_back, seekIncrementSeconds),
                size = seekSize,
                onClick = actions.onSeekBack,
            )
        }
        Box(Modifier.size(mainSize), contentAlignment = Alignment.Center) {
            if (state.isBuffering && state.playWhenReady) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    modifier = Modifier
                        .size(mainSize * 0.75f)
                        .semantics { contentDescription = "" },
                )
            }
            val playing = state.playWhenReady && state.status != com.cineverse.core.player.model.PlaybackStatus.Ended
            val label = stringResource(if (playing) R.string.player_pause else R.string.player_play)
            val stateLabel = stringResource(
                when {
                    state.isBuffering && state.playWhenReady -> R.string.player_state_buffering
                    playing -> R.string.player_state_playing
                    else -> R.string.player_state_paused
                },
            )
            IconButton(
                onClick = actions.onTogglePlayPause,
                modifier = Modifier
                    .size(mainSize)
                    .semantics { stateDescription = stateLabel },
            ) {
                Icon(
                    if (playing) icons.pause else icons.play,
                    contentDescription = label,
                    modifier = Modifier.size(mainSize * 0.6f),
                )
            }
        }
        if (controls.showSeekButtons && state.canSeek) {
            SeekButton(
                icon = icons.seekForward,
                seconds = seekIncrementSeconds,
                description = stringResource(R.string.player_seek_forward, seekIncrementSeconds),
                size = seekSize,
                onClick = actions.onSeekForward,
            )
        }
    }
}

@Composable
private fun SeekButton(icon: ImageVector, seconds: Int, description: String, size: Dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(size * 0.7f))
            Text(
                seconds.toString(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = size * 0.08f).clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun BottomBar(
    state: PlayerState,
    scrubPositionMs: Long?,
    isFullscreen: Boolean,
    icons: PlayerIcons,
    colors: PlayerColors,
    accent: Color,
    controls: PlayerControlsConfig,
    slots: PlayerSlots,
    slotScope: PlayerSlotScope,
    actions: ControlActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        slots.bottomBar?.invoke(slotScope)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val position = scrubPositionMs ?: state.positionMs
            if (state.isLive) {
                LiveBadge(state.isAtLiveEdge, colors.liveBadge)
                if (state.canSeek && !state.isAtLiveEdge) {
                    Text(
                        stringResource(
                            R.string.player_behind_live,
                            formatTime(state.liveBehindMs + (state.positionMs - position)),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else if (state.durationMs > 0) {
                Text(
                    "${formatTime(position)} / ${formatTime(state.durationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            if (state.isLive && !state.isAtLiveEdge) {
                TextButton(onClick = actions.onGoToLive, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                    Text(stringResource(R.string.player_go_to_live), color = colors.content)
                }
            }
            if (controls.showFullscreen) {
                PlayerIconButton(
                    if (isFullscreen) icons.exitFullscreen else icons.enterFullscreen,
                    stringResource(if (isFullscreen) R.string.player_exit_fullscreen else R.string.player_enter_fullscreen),
                    actions.onToggleFullscreen,
                )
            }
        }
        if (state.canSeek) {
            SeekBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedPositionMs = state.bufferedPositionMs,
                accent = accent,
                bufferedColor = colors.bufferedTrack,
                inactiveColor = colors.inactiveTrack,
                onSeek = actions.onSeek,
                onScrub = actions.onScrub,
            )
        }
    }
}

@Composable
private fun LiveBadge(atLiveEdge: Boolean, color: Color) {
    val state = stringResource(if (atLiveEdge) R.string.player_live_at_edge else R.string.player_live_behind)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(if (atLiveEdge) color else Color.Gray, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) { stateDescription = state },
    ) {
        Box(Modifier.size(6.dp).background(Color.White, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.player_live), style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
internal fun PlayerIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
internal fun ErrorOverlay(
    message: String?,
    icons: PlayerIcons,
    colors: PlayerColors,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides colors.content) {
        Box(modifier.fillMaxSize().background(colors.videoBackground.copy(alpha = 0.85f))) {
            PlayerIconButton(icons.back, stringResource(R.string.player_back), onBack, Modifier.padding(4.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            ) {
                Text(stringResource(R.string.player_error_title), style = MaterialTheme.typography.titleMedium)
                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.content, contentColor = colors.videoBackground),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(icons.retry, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.player_retry))
                }
            }
        }
    }
}

/** Brief "+10s" / "−10s" bubble on the side that was double-tapped. */
@Composable
internal fun SeekFeedback(forward: Boolean, seconds: Int, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = if (forward) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            stringResource(if (forward) R.string.player_seek_feedback_forward else R.string.player_seek_feedback_back, seconds),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
