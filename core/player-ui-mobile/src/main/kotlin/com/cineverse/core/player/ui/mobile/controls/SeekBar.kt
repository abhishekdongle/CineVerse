package com.cineverse.core.player.ui.mobile.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cineverse.core.player.ui.mobile.R

/**
 * Seek bar with buffered progress. Dragging previews the position (shown via [onScrub]) and
 * seeks once on release, so playback doesn't stutter during the drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    accent: Color,
    bufferedColor: Color,
    inactiveColor: Color,
    onSeek: (Long) -> Unit,
    onScrub: (positionMs: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val fraction = scrubFraction ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPositionMs.toFloat() / duration).coerceIn(0f, 1f)
    val description = stringResource(R.string.player_seek_bar)
    val position = stringResource(
        R.string.player_position_of_duration,
        formatTime((fraction * duration).toLong()),
        formatTime(durationMs),
    )

    Slider(
        value = fraction,
        onValueChange = {
            scrubFraction = it
            onScrub((it * duration).toLong())
        },
        onValueChangeFinished = {
            scrubFraction?.let { onSeek((it * duration).toLong()) }
            scrubFraction = null
            onScrub(null)
        },
        colors = SliderDefaults.colors(thumbColor = accent),
        track = {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(inactiveColor, cornerRadius = radius)
                drawRoundRect(bufferedColor, size = Size(size.width * bufferedFraction, size.height), cornerRadius = radius)
                drawRoundRect(accent, topLeft = Offset.Zero, size = Size(size.width * fraction, size.height), cornerRadius = radius)
            }
        },
        modifier = modifier.semantics {
            contentDescription = description
            stateDescription = position
        },
    )
}
