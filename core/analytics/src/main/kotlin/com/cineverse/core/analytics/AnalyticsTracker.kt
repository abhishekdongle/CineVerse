package com.cineverse.core.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
