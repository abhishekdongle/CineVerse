package com.cineverse.core.notifications

import com.cineverse.core.common.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CineVerseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHandler: PushNotificationHandler

    @Inject lateinit var tokenRegistrar: PushTokenRegistrar

    // firebase-messaging 26 replaced onNewToken with onRegistered/onUnregistered.
    override fun onRegistered(registrationId: String) {
        tokenRegistrar.register(registrationId)
    }

    override fun onUnregistered(registrationId: String) {
        tokenRegistrar.unregister(registrationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: return
        val body = message.notification?.body ?: data["body"].orEmpty()
        notificationHandler.show(
            PushMessage(
                title = title,
                body = body,
                channelId = data["channel"] ?: Constants.NOTIFICATION_CHANNEL_GENERAL,
                deepLink = data["deep_link"],
            ),
        )
    }
}
