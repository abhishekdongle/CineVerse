package com.cineverse.core.analytics.di

import com.cineverse.core.analytics.AnalyticsTracker
import com.cineverse.core.analytics.LogcatAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindsAnalyticsTracker(impl: LogcatAnalyticsTracker): AnalyticsTracker
}
