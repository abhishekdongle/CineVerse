package com.cineverse.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cineverse.core.common.extensions.appLaunchIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface PushNotificationHandler {
    fun show(message: PushMessage)
}

internal class SystemPushNotificationHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PushNotificationHandler {

    override fun show(message: PushMessage) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val intent = message.deepLink
            ?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)).setPackage(context.packageName) }
            ?: context.appLaunchIntent()
            ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            message.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, message.channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(message.hashCode(), notification)
    }
}
