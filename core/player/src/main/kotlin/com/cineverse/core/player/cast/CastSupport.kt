package com.cineverse.core.player.cast

import android.content.Context
import android.util.Log
import androidx.media3.cast.Cast
import androidx.media3.cast.CastParams
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Optional Google Cast support. Cast needs Google Play services, which Fire TV and some other
 * devices lack; there [isAvailable] stays false, no CastPlayer is created and UIs hide the
 * Cast button.
 */
object CastSupport {

    private const val TAG = "CastSupport"

    @Volatile
    private var initialized = false

    /** Call once from Application.onCreate on the main process. Safe on devices without Play services. */
    fun initialize(context: Context) {
        if (initialized) return
        val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (playServices != ConnectionResult.SUCCESS) {
            Log.i(TAG, "Google Play services unavailable ($playServices); Cast disabled")
            return
        }
        initialized = try {
            // The Default Media Receiver plays HLS and DASH without a custom receiver app.
            Cast.getSingletonInstance(context).initialize(
                CastParams.Builder().setRemoteToLocalEnabled(true).build(),
            )
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Cast initialization failed; Cast disabled", e)
            false
        }
    }

    /** True when Cast was initialized and the Cast framework loaded (or is still loading). */
    fun isAvailable(context: Context): Boolean =
        initialized && Cast.getSingletonInstance(context).castContextLoadFailure == null

    internal fun currentDeviceName(context: Context): String? =
        if (initialized) {
            runCatching {
                Cast.getSingletonInstance(context).currentCastSession?.castDevice?.friendlyName
            }.getOrNull()
        } else {
            null
        }
}
