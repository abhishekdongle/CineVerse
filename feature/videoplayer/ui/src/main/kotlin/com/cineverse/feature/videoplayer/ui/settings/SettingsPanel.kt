package com.cineverse.feature.videoplayer.ui.settings

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
import com.cineverse.feature.videoplayer.presentation.SettingsPage
import com.cineverse.feature.videoplayer.presentation.SettingsUiState
import com.cineverse.feature.videoplayer.presentation.VideoPlayerAction
import com.cineverse.feature.videoplayer.ui.PlayerControlsConfig
import com.cineverse.feature.videoplayer.ui.R
import java.text.DecimalFormat

private data class Option(val label: String, val selected: Boolean, val action: VideoPlayerAction)

/**
 * Quality, audio, subtitle and speed settings. A bottom sheet on compact portrait screens and a
 * side panel in landscape or on larger screens. Each choice applies immediately to the running
 * player; nothing restarts playback. Drawn in the player's own layout rather than a dialog, so it
 * stays inside immersive mode.
 */
@Composable
internal fun SettingsPanel(
    settings: SettingsUiState,
    controls: PlayerControlsConfig,
    accent: Color,
    onAction: (VideoPlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = settings.isVisible
    val asSidePanel = settings.asSidePanel
    val page = settings.page
    val onDismiss = { onAction(VideoPlayerAction.SettingsDismissed) }
    BackHandler(enabled = visible) { onAction(VideoPlayerAction.SettingsBackPressed) }

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
                            SettingsPage.Main -> MainPage(settings, controls, onOpen = { onAction(VideoPlayerAction.SettingsPageOpened(it)) })
                            else -> OptionsPage(
                                title = pageTitle(page),
                                options = options(page, settings),
                                accent = accent,
                                onBack = { onAction(VideoPlayerAction.SettingsBackPressed) },
                                onSelect = onAction,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainPage(settings: SettingsUiState, controls: PlayerControlsConfig, onOpen: (SettingsPage) -> Unit) {
    Text(
        stringResource(R.string.player_settings),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).semantics { heading() },
    )
    if (controls.showQuality) {
        SettingRow(stringResource(R.string.player_settings_quality), qualityValue(settings), enabled = settings.canChangeQuality) { onOpen(SettingsPage.Quality) }
    }
    if (controls.showAudio) {
        SettingRow(
            stringResource(R.string.player_settings_audio),
            settings.selectedAudio?.label ?: stringResource(R.string.player_settings_none),
            enabled = settings.canChangeAudio,
        ) { onOpen(SettingsPage.Audio) }
    }
    if (controls.showSubtitles) {
        SettingRow(
            stringResource(R.string.player_settings_subtitles),
            settings.selectedSubtitle?.label ?: stringResource(R.string.player_subtitles_off),
            enabled = settings.canChangeSubtitles,
        ) { onOpen(SettingsPage.Subtitles) }
    }
    if (controls.showSpeed) {
        SettingRow(
            stringResource(R.string.player_settings_speed),
            if (settings.canChangeSpeed) speedLabel(settings.currentSpeed) else stringResource(R.string.player_speed_unavailable_live),
            enabled = settings.canChangeSpeed,
        ) { onOpen(SettingsPage.Speed) }
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
    onSelect: (VideoPlayerAction) -> Unit,
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
                    .selectable(selected = option.selected, role = Role.RadioButton) { onSelect(option.action) }
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
private fun pageTitle(page: SettingsPage): String = stringResource(
    when (page) {
        SettingsPage.Quality -> R.string.player_settings_quality
        SettingsPage.Audio -> R.string.player_settings_audio
        SettingsPage.Subtitles -> R.string.player_settings_subtitles
        SettingsPage.Speed, SettingsPage.Main -> R.string.player_settings_speed
    },
)

@Composable
private fun options(page: SettingsPage, settings: SettingsUiState): List<Option> = when (page) {
    SettingsPage.Quality -> settings.qualities.map {
        val label = it.height?.let { height -> stringResource(R.string.player_quality_height, height) }
            ?: stringResource(R.string.player_quality_auto)
        Option(label, it.isSelected, VideoPlayerAction.QualitySelected(it.id))
    }

    SettingsPage.Audio -> settings.audioTracks.mapNotNull { track ->
        track.id?.let { Option(track.label.orEmpty(), track.isSelected, VideoPlayerAction.AudioSelected(it)) }
    }

    SettingsPage.Subtitles -> settings.subtitleTracks.map {
        Option(it.label ?: stringResource(R.string.player_subtitles_off), it.isSelected, VideoPlayerAction.SubtitleSelected(it.id))
    }

    SettingsPage.Speed -> settings.speeds.map { Option(speedLabel(it.speed), it.isSelected, VideoPlayerAction.SpeedSelected(it.speed)) }
    SettingsPage.Main -> emptyList()
}

@Composable
private fun qualityValue(settings: SettingsUiState): String {
    val selectedHeight = settings.selectedQuality?.height
    val autoHeight = settings.autoQualityHeight
    return when {
        selectedHeight != null -> stringResource(R.string.player_quality_height, selectedHeight)
        autoHeight != null -> stringResource(R.string.player_quality_auto_current, autoHeight)
        else -> stringResource(R.string.player_quality_auto)
    }
}

@Composable
private fun speedLabel(speed: Float): String =
    if (speed == 1f) stringResource(R.string.player_speed_normal) else stringResource(R.string.player_speed_value, DecimalFormat("0.##").format(speed))
