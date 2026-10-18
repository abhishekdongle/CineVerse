package com.cineverse.core.player.ui.mobile.system

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Orientation handling for fullscreen on phones:
 * - Entering fullscreen with a landscape video rotates to landscape (USER_LANDSCAPE), even when
 *   rotation is locked, because the user asked for it.
 * - Leaving fullscreen while in landscape rotates back to portrait.
 * - Once the device is physically held in the requested orientation, the lock is released so
 *   the sensor takes over again, but only when auto-rotate is on. With rotation locked, the
 *   system lock is respected.
 * Tablets and foldables are never rotated.
 */
internal class FullscreenOrientation(private val activity: Activity) {

    private enum class Pending { None, Landscape, Portrait }

    private var pending = Pending.None

    private val listener = object : OrientationEventListener(activity) {
        override fun onOrientationChanged(degrees: Int) {
            if (degrees == ORIENTATION_UNKNOWN || pending == Pending.None) return
            val reached = when (pending) {
                Pending.Landscape -> degrees in 60..120 || degrees in 240..300
                Pending.Portrait -> degrees <= 30 || degrees >= 330
                Pending.None -> false
            }
            if (reached && isAutoRotateEnabled()) release()
        }
    }

    fun enter(rotateToLandscape: Boolean) {
        if (!rotateToLandscape) return
        request(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, Pending.Landscape)
    }

    fun exit(isLandscapeNow: Boolean) {
        if (!isLandscapeNow) {
            release()
            return
        }
        request(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, Pending.Portrait)
        // With rotation locked, UNSPECIFIED already means the locked orientation.
        if (!isAutoRotateEnabled()) release()
    }

    fun release() {
        pending = Pending.None
        listener.disable()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun request(orientation: Int, next: Pending) {
        pending = next
        activity.requestedOrientation = orientation
        if (listener.canDetectOrientation()) listener.enable()
    }

    private fun isAutoRotateEnabled(): Boolean =
        Settings.System.getInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
}

@Composable
internal fun rememberFullscreenOrientation(activity: Activity?): FullscreenOrientation? {
    val orientation = remember(activity) { activity?.let(::FullscreenOrientation) }
    DisposableEffect(orientation) {
        onDispose { orientation?.release() }
    }
    return orientation
}

/** Hides the status and navigation bars while [immersive]; swiping from an edge shows them briefly. */
@Composable
internal fun ImmersiveModeEffect(activity: Activity?, immersive: Boolean) {
    DisposableEffect(activity, immersive) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
