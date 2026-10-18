package com.cineverse.core.notifications.di

import com.cineverse.core.notifications.LoggingPushTokenRegistrar
import com.cineverse.core.notifications.PushNotificationHandler
import com.cineverse.core.notifications.PushTokenRegistrar
import com.cineverse.core.notifications.SystemPushNotificationHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {

    @Binds
    abstract fun bindsPushNotificationHandler(impl: SystemPushNotificationHandler): PushNotificationHandler

    @Binds
    abstract fun bindsPushTokenRegistrar(impl: LoggingPushTokenRegistrar): PushTokenRegistrar
}
