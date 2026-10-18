package com.cineverse.core.analytics

import android.util.Log
import javax.inject.Inject

/** Default implementation: writes events to Logcat. Replace with your analytics backend. */
internal class LogcatAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        Log.d(TAG, "${event.name} ${event.params}")
    }

    private companion object {
        const val TAG = "CineVerseAnalytics"
    }
}
