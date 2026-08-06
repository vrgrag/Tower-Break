package com.towerbreak.towerbreakgame.ignite

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.towerbreak.towerbreakgame.BuildConfig
import com.towerbreak.towerbreakgame.R
import com.towerbreak.towerbreakgame.trail.Trace
import com.towerbreak.towerbreakgame.pane.BastionGate
import com.towerbreak.towerbreakgame.link.BastionVault
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Firebase Cloud Messaging receiver. The class name, package and channel id
 * are per-project (`.cursor/rules/kotlin_fingerprint.mdc`) — reached through
 * [BuildConfig] rather than hardcoded here.
 *
 * URL handling:
 *   * No host allowlist. A push comes from this app's own FCM project; the
 *     allowlist meant for config-endpoint answers used to be applied here and
 *     silently dropped every campaign link on an unlisted host, leaving the
 *     launcher to route the tap as an ordinary start.
 *   * A shell on screen takes the URL through [BastionBus] and no notification
 *     is posted at all.
 *   * Otherwise the URL is stashed in the vault and the tap opens the launcher,
 *     which hands it to a live shell or opens one on it.
 *   * A user whose channel is NATIVE keeps their game: the URL becomes a
 *     harmless notification, and the tap opens the launcher instead of the
 *     WebView. Flipping a NATIVE user into a WebView after the fact is a
 *     store-review problem, not a feature.
 *
 * Image fetch runs on a background scope, so a slow image URL never blocks
 * the FCM service's main-thread callback.
 */
class BastionFcm : FirebaseMessagingService() {

    private val bg = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        bg.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        BastionVault(applicationContext).fcmToken = token
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        val data = msg.data
        val notif = msg.notification

        // A missing title or body is not a reason to drop the message: the URL
        // is the point of the push, and an empty field only means the sender
        // relies on the defaults. Returning here (previous behaviour) threw the
        // destination away before anything had a chance to route on it.
        val title = data["title"] ?: notif?.title ?: getString(R.string.app_name)
        val body  = data["body"]  ?: notif?.body  ?: ""
        val imgUrl = data["image"] ?: notif?.imageUrl?.toString() ?: ""

        val url = pickUrl(data, notif)
        if (body.isBlank() && url.isEmpty()) return
        if (url.isEmpty()) Trace.w(TAG, "push has no URL — data keys: ${data.keys.joinToString()}")

        val vault = BastionVault(applicationContext)
        val native = vault.runChannel == BastionVault.RunChannel.NATIVE

        // Shell on screen: load it in place and post nothing. Gated on the live
        // callback rather than the run channel — a debug forced session never
        // writes STREAM, and the channel gate would drop it (pitfalls #36).
        // A NATIVE install has no shell, so the lock below still holds.
        if (url.isNotEmpty() && !native && BastionBus.onWarmUrl != null) {
            val delivered = runCatching { BastionBus.handOver(url) }.getOrDefault(false)
            if (delivered) return
        }

        // The URL a tap may carry. A NATIVE user gets the text only: the
        // launcher never sees the URL and never routes on it. Flipping a NATIVE
        // install into a WebView after the fact is a store-review problem.
        val tapUrl = if (native) "" else url

        // Stashed unconditionally. Gating this on `!shellAlive` (previous
        // behaviour) lost the destination whenever the process was recycled
        // between the message arriving and the user tapping — the launcher then
        // started cold with nothing to route on. Every consumer takes it
        // exactly once, and the live-shell path in BastionGate clears it.
        if (tapUrl.isNotEmpty()) vault.coldPushUrl = tapUrl

        bg.launch { showNotification(title, body, tapUrl, imgUrl) }
    }

    /** Backends label the destination differently; take the first key holding one. */
    private fun pickUrl(data: Map<String, String>, notif: RemoteMessage.Notification?): String {
        URL_KEYS.firstNotNullOfOrNull { data[it]?.trim()?.takeIf(::isWebUrl) }?.let { return it }
        data.entries.firstOrNull { (k, v) ->
            !k.contains("image", true) && !k.contains("icon", true) && isWebUrl(v.trim())
        }?.let { return it.value.trim() }
        return notif?.clickAction?.trim()?.takeIf(::isWebUrl) ?: ""
    }

    private suspend fun showNotification(
        title: String,
        body: String,
        url: String,
        imgUrl: String,
    ) {
        val ctx = applicationContext
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        BastionChannel.ensure(ctx)

        val tap = tapIntent(ctx, url)
        // A per-notification request code. Sharing one across notifications lets
        // FLAG_UPDATE_CURRENT rewrite the extras of a PendingIntent that is still
        // pending, so an older notification would open the newest URL.
        val pi = PendingIntent.getActivity(
            ctx,
            System.currentTimeMillis().toInt(),
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(ctx, BuildConfig.FCM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_tower)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val bitmap = if (imgUrl.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                URL(imgUrl).openConnection().apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }.getInputStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }

        if (bitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        withContext(Dispatchers.Main) {
            nm.notify(NOTIF_ID++, builder.build())
        }
    }

    /**
     * Every tap goes through the launcher, whether the shell is alive or not.
     *
     * Targeting BastionShell directly with a PendingIntent (previous design)
     * relies on the OS to deliver an intent into a singleTask activity that
     * is not the task root; several OEM ROMs relaunch the task from its root
     * instead, which is exactly the "app restarts on notification tap" the
     * user reported. BastionGate is the task root, so a tap always resolves
     * onto it — and its onCreate hands the URL to the live shell via
     * BastionBus without ever drawing its own splash (see BastionGate).
     */
    private fun tapIntent(ctx: Context, url: String): Intent =
        Intent(ctx, BastionGate::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (url.isNotBlank()) putExtra(BastionGate.EXTRA_PUSH_URL, url)
            putExtra(BastionGate.EXTRA_FROM_PUSH, true)
        }

    private fun isWebUrl(value: String?): Boolean =
        !value.isNullOrBlank() &&
            (value.startsWith("http://", true) || value.startsWith("https://", true))

    companion object {
        private const val TAG = "BastionFcm"
        private const val NOTIF_REQ = 42001
        @Volatile private var NOTIF_ID = 1001

        /** Data keys checked before falling back to scanning the whole payload. */
        private val URL_KEYS = listOf("url", "link", "deeplink", "deep_link", "target_url")
    }
}
