package com.cineverse.core.notifications

import android.util.Log
import javax.inject.Inject

/**
 * Tells the backend which FCM registration this device has, so it can target pushes.
 * Replace the logging implementation once that API exists.
 */
interface PushTokenRegistrar {
    fun register(registrationId: String)

    fun unregister(registrationId: String)
}

internal class LoggingPushTokenRegistrar @Inject constructor() : PushTokenRegistrar {
    override fun register(registrationId: String) {
        Log.d(TAG, "Registered with FCM (${registrationId.length} chars)")
    }

    override fun unregister(registrationId: String) {
        Log.d(TAG, "Unregistered from FCM; the backend should drop this id")
    }

    private companion object {
        const val TAG = "CineVersePush"
    }
}
