package com.cineverse.mobile.analytics

import com.cineverse.core.analytics.AnalyticsEvent
import com.cineverse.core.analytics.AnalyticsTracker
import com.cineverse.core.player.analytics.PlaybackAnalyticsListener
import com.cineverse.core.player.model.PlaybackRequest
import com.cineverse.core.player.model.PlayerError
import com.cineverse.core.player.model.VideoQuality
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/** Forwards player events to the app's analytics. :core:player itself knows nothing about it. */
class PlayerAnalyticsBridge @Inject constructor(
    private val tracker: AnalyticsTracker,
) : PlaybackAnalyticsListener {

    override fun onPlaybackStarted(request: PlaybackRequest) = track(AnalyticsEvent.Types.PLAYBACK_STARTED, request)

    override fun onQualityChanged(request: PlaybackRequest, quality: VideoQuality) =
        track("playback_quality_changed", request, "height" to quality.height.toString())

    override fun onPlaybackError(request: PlaybackRequest, error: PlayerError) =
        track(AnalyticsEvent.Types.PLAYBACK_ERROR, request, AnalyticsEvent.Params.ERROR_CODE to error.codeName)

    override fun onProgress(request: PlaybackRequest, positionMs: Long, durationMs: Long) =
        track("playback_progress", request, "position_s" to (positionMs / 1000).toString())

    private fun track(name: String, request: PlaybackRequest, vararg extra: Pair<String, String>) {
        tracker.track(AnalyticsEvent(name, mapOf(AnalyticsEvent.Params.CONTENT_ID to request.id) + extra))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackAnalyticsModule {
    @Binds
    @IntoSet
    abstract fun bindsPlayerAnalyticsBridge(impl: PlayerAnalyticsBridge): PlaybackAnalyticsListener
}
