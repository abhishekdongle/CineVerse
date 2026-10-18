package com.cineverse.core.player.di

import com.cineverse.core.player.analytics.PlaybackAnalyticsListener
import com.cineverse.core.player.analytics.ProgressReporter
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/** Declares the hook sets so apps that bind nothing still get empty sets. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlayerModule {

    @Multibinds
    abstract fun playbackAnalyticsListeners(): Set<PlaybackAnalyticsListener>

    @Multibinds
    abstract fun progressReporters(): Set<ProgressReporter>
}
