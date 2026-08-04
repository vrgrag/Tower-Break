package com.towerbreak.towerbreakgame.ignite

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.towerbreak.towerbreakgame.BuildConfig

/**
 * Owns the one notification channel this app posts to.
 *
 * This has to exist before the first notification, and creating it lazily inside
 * the messaging service is not early enough. A push that carries a `notification`
 * block is drawn by the Firebase SDK itself whenever the app is not in the
 * foreground, which is the common case — the service's `onMessageReceived` never
 * runs, so nothing would have created the channel named by the manifest's
 * `default_notification_channel_id`. On API 26+ a post to a channel that does not
 * exist is dropped without a trace, which looks exactly like "push is broken".
 *
 * Registering a channel is idempotent, so [ensure] is safe to call from both the
 * Application and the service.
 */
internal object BastionChannel {

    fun ensure(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(BuildConfig.FCM_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                BuildConfig.FCM_CHANNEL_ID,
                BuildConfig.FCM_CHANNEL_TITLE,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
        )
    }
}
