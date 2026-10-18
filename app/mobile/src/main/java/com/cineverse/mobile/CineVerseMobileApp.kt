package com.cineverse.mobile

import android.app.Application
import com.cineverse.core.notifications.NotificationChannels
import com.cineverse.core.player.cast.CastSupport
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CineVerseMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        // No-op without Google Play services (e.g. Fire tablets); the Cast button stays hidden.
        CastSupport.initialize(this)
    }
}
