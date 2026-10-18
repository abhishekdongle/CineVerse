package com.cineverse.mobile

import android.app.Application
import com.cineverse.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineVerseMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
