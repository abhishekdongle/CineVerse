package com.cineverse.core.player.di

import com.cineverse.core.player.CineVersePlayer
import com.cineverse.core.player.PlayerControllerFactory
import com.cineverse.core.player.analytics.PlaybackAnalyticsListener
import com.cineverse.core.player.analytics.ProgressReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Binds the Media3 implementation behind [PlayerControllerFactory] and declares the hook sets,
 * so apps that bind nothing still get empty sets.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlayerModule {

    @Binds
    abstract fun bindsPlayerControllerFactory(factory: CineVersePlayer.Factory): PlayerControllerFactory

    @Multibinds
    abstract fun playbackAnalyticsListeners(): Set<PlaybackAnalyticsListener>

    @Multibinds
    abstract fun progressReporters(): Set<ProgressReporter>
}
