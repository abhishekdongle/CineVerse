package com.cineverse.core.player.ui.mobile.system

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import com.cineverse.core.player.ui.mobile.R

private const val TAG = "PlayerPiP"
private const val ACTION_PIP_CONTROL = "com.cineverse.core.player.ui.mobile.PIP_CONTROL"
private const val EXTRA_CONTROL = "control"
private const val CONTROL_PLAY = 1
private const val CONTROL_PAUSE = 2

/** True while the activity is in Picture-in-Picture. */
@Composable
internal fun rememberIsInPictureInPicture(activity: ComponentActivity?): Boolean {
    var inPip by remember(activity) {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity?.isInPictureInPictureMode == true)
    }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        activity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity?.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPip
}

/**
 * Keeps Picture-in-Picture in sync with playback:
 * - Android 12+: auto-enter is enabled while playing, so swiping home enters PiP smoothly.
 * - Android 7–11: enters PiP from onUserLeaveHint while playing.
 * - Aspect ratio follows the video; a play/pause remote action is shown in the PiP window.
 * Everything is reset when the player leaves composition, so other screens never enter PiP.
 */
@Composable
internal fun PictureInPictureEffect(
    activity: ComponentActivity?,
    enabled: Boolean,
    isPlaying: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    sourceRectHint: Rect?,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
    if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return

    val currentIsPlaying by rememberUpdatedState(isPlaying && enabled)
    val currentOnPlay by rememberUpdatedState(onPlay)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentVideoSize by rememberUpdatedState(videoWidth to videoHeight)
    val currentSourceRect by rememberUpdatedState(sourceRectHint)

    // Remote actions (play/pause buttons inside the PiP window).
    DisposableEffect(activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getIntExtra(EXTRA_CONTROL, 0)) {
                    CONTROL_PLAY -> currentOnPlay()
                    CONTROL_PAUSE -> currentOnPause()
                }
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { activity.unregisterReceiver(receiver) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LaunchedEffect(activity, enabled, isPlaying, videoWidth, videoHeight, sourceRectHint) {
            activity.trySetPipParams(buildParams(activity, isPlaying && enabled, isPlaying, videoWidth, videoHeight, sourceRectHint))
        }
    }

    // Before Android 12 there is no auto-enter; enter manually when the user leaves the app.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        DisposableEffect(activity) {
            val listener = Runnable {
                if (!currentIsPlaying) return@Runnable
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.enterPictureInPictureMode(
                            buildParams(activity, false, true, currentVideoSize.first, currentVideoSize.second, currentSourceRect),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        activity.enterPictureInPictureMode()
                    }
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Activity does not support Picture-in-Picture", e)
                }
            }
            activity.addOnUserLeaveHintListener(listener)
            onDispose { activity.removeOnUserLeaveHintListener(listener) }
        }
    }

    // Leaving the player: stop auto-enter so other screens don't go into PiP.
    DisposableEffect(activity) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.trySetPipParams(buildParams(activity, false, false, 0, 0, null, withActions = false))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildParams(
    context: Context,
    autoEnter: Boolean,
    isPlaying: Boolean,
    videoWidth: Int,
    videoHeight: Int,
    sourceRectHint: Rect?,
    withActions: Boolean = true,
): PictureInPictureParams {
    val builder = PictureInPictureParams.Builder()
    aspectRatio(videoWidth, videoHeight)?.let(builder::setAspectRatio)
    sourceRectHint?.let(builder::setSourceRectHint)
    builder.setActions(if (withActions) listOf(playPauseAction(context, isPlaying)) else emptyList())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(autoEnter)
        builder.setSeamlessResizeEnabled(true)
    }
    return builder.build()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun playPauseAction(context: Context, isPlaying: Boolean): RemoteAction {
    val control = if (isPlaying) CONTROL_PAUSE else CONTROL_PLAY
    val title = context.getString(if (isPlaying) R.string.player_pause else R.string.player_play)
    val icon = Icon.createWithResource(context, if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play)
    val intent = PendingIntent.getBroadcast(
        context,
        control,
        Intent(ACTION_PIP_CONTROL).setPackage(context.packageName).putExtra(EXTRA_CONTROL, control),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    return RemoteAction(icon, title, title, intent)
}

/** PiP accepts aspect ratios between 1:2.39 and 2.39:1; clamp to stay inside that range. */
private fun aspectRatio(width: Int, height: Int): Rational? {
    if (width <= 0 || height <= 0) return null
    val ratio = width.toFloat() / height
    return when {
        ratio > 2.39f -> Rational(239, 100)
        ratio < 1 / 2.39f -> Rational(100, 239)
        else -> Rational(width, height)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun ComponentActivity.trySetPipParams(params: PictureInPictureParams) {
    try {
        setPictureInPictureParams(params)
    } catch (e: IllegalStateException) {
        // The activity is missing android:supportsPictureInPicture="true".
        Log.w(TAG, "Activity does not support Picture-in-Picture", e)
    }
}
