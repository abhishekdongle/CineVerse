package com.cineverse.core.player.ui.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cineverse.core.player.PlayerController
import com.cineverse.core.player.TrackLabels
import com.cineverse.core.player.model.PlayerState
import com.cineverse.core.player.ui.mobile.PlayerControlsConfig
import com.cineverse.core.player.ui.mobile.R
import java.text.DecimalFormat

private enum class Page { Main, Quality, Audio, Subtitles, Speed }

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private data class Option(val key: String?, val label: String, val selected: Boolean)

/**
 * Quality, audio, subtitle and speed settings. A bottom sheet on compact portrait screens and a
 * side panel in landscape or on larger screens. Each choice applies immediately to the running
 * player; nothing restarts playback. Drawn in the player's own layout rather than a dialog, so it
 * stays inside immersive mode.
 */
@Composable
internal fun SettingsPanel(
    visible: Boolean,
    asSidePanel: Boolean,
    state: PlayerState,
    controller: PlayerController,
    controls: PlayerControlsConfig,
    accent: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(Page.Main) }
    LaunchedEffect(visible) { if (!visible) page = Page.Main }
    BackHandler(enabled = visible) {
        if (page == Page.Main) onDismiss() else page = Page.Main
    }

    val closeLabel = stringResource(R.string.player_close_settings)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClickLabel = closeLabel, onClick = onDismiss)
                    .semantics { contentDescription = closeLabel },
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = if (asSidePanel) slideInHorizontally { it } else slideInVertically { it },
            exit = if (asSidePanel) slideOutHorizontally { it } else slideOutVertically { it },
            modifier = Modifier.align(if (asSidePanel) Alignment.CenterEnd else Alignment.BottomCenter),
        ) {
            BoxWithConstraints {
                val title = stringResource(R.string.player_settings)
                Surface(
                    shape = if (asSidePanel) RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    tonalElevation = 3.dp,
                    modifier = (if (asSidePanel) Modifier.width(340.dp).fillMaxHeight() else Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.7f))
                        .semantics { paneTitle = title },
                ) {
                    Column(
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                    ) {
                        when (page) {
                            Page.Main -> MainPage(state, controls, onOpen = { page = it })
                            else -> OptionsPage(
                                title = pageTitle(page),
                                options = options(page, state),
                                accent = accent,
                                onBack = { page = Page.Main },
                                onSelect = { key ->
                                    apply(page, key, controller)
                                    page = Page.Main
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPage(state: PlayerState, controls: PlayerControlsConfig, onOpen: (Page) -> Unit) {
    Text(
        stringResource(R.string.player_settings),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).semantics { heading() },
    )
    if (controls.showQuality) {
        SettingRow(stringResource(R.string.player_settings_quality), qualityValue(state), enabled = state.videoQualities.isNotEmpty()) { onOpen(Page.Quality) }
    }
    if (controls.showAudio) {
        val names = TrackLabels.audioNames(state.audioTracks)
        SettingRow(
            stringResource(R.string.player_settings_audio),
            state.selectedAudioTrackId?.let(names::get) ?: stringResource(R.string.player_settings_none),
            enabled = state.audioTracks.size > 1,
        ) { onOpen(Page.Audio) }
    }
    if (controls.showSubtitles) {
        val names = TrackLabels.subtitleNames(state.subtitleTracks)
        SettingRow(
            stringResource(R.string.player_settings_subtitles),
            state.selectedSubtitleTrackId?.let(names::get) ?: stringResource(R.string.player_subtitles_off),
            enabled = state.subtitleTracks.isNotEmpty(),
        ) { onOpen(Page.Subtitles) }
    }
    if (controls.showSpeed) {
        SettingRow(
            stringResource(R.string.player_settings_speed),
            if (state.canChangeSpeed) speedLabel(state.playbackSpeed) else stringResource(R.string.player_speed_unavailable_live),
            enabled = state.canChangeSpeed,
        ) { onOpen(Page.Speed) }
    }
}

@Composable
private fun SettingRow(label: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        val alpha = if (enabled) 1f else 0.5f
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        if (enabled) {
            Icon(ImageVector.vectorResource(R.drawable.ic_player_chevron), contentDescription = null)
        }
    }
}

@Composable
private fun OptionsPage(
    title: String,
    options: List<Option>,
    accent: Color,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
        androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(ImageVector.vectorResource(R.drawable.ic_player_back), contentDescription = stringResource(R.string.player_back))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    }
    HorizontalDivider()
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 52.dp)
                    .selectable(selected = option.selected, role = Role.RadioButton) { onSelect(option.key) }
                    .padding(horizontal = 24.dp),
            ) {
                Box(Modifier.size(24.dp)) {
                    if (option.selected) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_player_check), contentDescription = null, tint = accent)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun pageTitle(page: Page): String = stringResource(
    when (page) {
        Page.Quality -> R.string.player_settings_quality
        Page.Audio -> R.string.player_settings_audio
        Page.Subtitles -> R.string.player_settings_subtitles
        Page.Speed, Page.Main -> R.string.player_settings_speed
    },
)

@Composable
private fun options(page: Page, state: PlayerState): List<Option> = when (page) {
    Page.Quality -> listOf(Option(null, stringResource(R.string.player_quality_auto), state.selectedVideoQualityId == null)) +
        state.videoQualities.map {
            Option(it.id, stringResource(R.string.player_quality_height, it.height), it.id == state.selectedVideoQualityId)
        }

    Page.Audio -> {
        val names = TrackLabels.audioNames(state.audioTracks)
        state.audioTracks.map { Option(it.id, names.getValue(it.id), it.id == state.selectedAudioTrackId) }
    }

    Page.Subtitles -> {
        val names = TrackLabels.subtitleNames(state.subtitleTracks)
        listOf(Option(null, stringResource(R.string.player_subtitles_off), state.selectedSubtitleTrackId == null)) +
            state.subtitleTracks.map { Option(it.id, names.getValue(it.id), it.id == state.selectedSubtitleTrackId) }
    }

    Page.Speed -> SPEEDS.map { Option(it.toString(), speedLabel(it), it == state.playbackSpeed) }
    Page.Main -> emptyList()
}

private fun apply(page: Page, key: String?, controller: PlayerController) {
    when (page) {
        Page.Quality -> controller.setQuality(key)
        Page.Audio -> key?.let(controller::setAudioTrack)
        Page.Subtitles -> controller.setSubtitleTrack(key)
        Page.Speed -> key?.toFloatOrNull()?.let(controller::setSpeed)
        Page.Main -> Unit
    }
}

@Composable
private fun qualityValue(state: PlayerState): String {
    val selected = state.videoQualities.firstOrNull { it.id == state.selectedVideoQualityId }
    val currentHeight = state.currentVideoHeight
    return when {
        selected != null -> stringResource(R.string.player_quality_height, selected.height)
        currentHeight != null -> stringResource(R.string.player_quality_auto_current, currentHeight)
        else -> stringResource(R.string.player_quality_auto)
    }
}

@Composable
private fun speedLabel(speed: Float): String =
    if (speed == 1f) stringResource(R.string.player_speed_normal) else stringResource(R.string.player_speed_value, DecimalFormat("0.##").format(speed))
