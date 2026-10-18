package com.cineverse.core.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.cineverse.core.common.Constants

object NotificationChannels {

    /** Call once from Application.onCreate. Creating a channel that already exists is a no-op. */
    fun createAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(
                    Constants.NOTIFICATION_CHANNEL_GENERAL,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ).setName(context.getString(R.string.notification_channel_general)).build(),
                NotificationChannelCompat.Builder(
                    Constants.NOTIFICATION_CHANNEL_LIVE_EVENTS,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ).setName(context.getString(R.string.notification_channel_live_events)).build(),
            ),
        )
    }
}
