package com.towerbreak.towerbreakgame.ignite

import android.content.Intent
import android.os.Bundle
import com.towerbreak.towerbreakgame.trail.Trace

/**
 * One resolver for "what URL is behind this notification tap", used by every
 * activity a tap can land on.
 *
 * A tap arrives in one of two shapes:
 *
 *  1. [BastionFcm] built the intent, so the URL sits in [EXTRA_PUSH_URL].
 *  2. The payload carried a `notification` block and the app was not in the
 *     foreground. Firebase drew the notification itself, our service never
 *     ran, and the tap opens the plain launcher intent with the whole `data`
 *     map flattened into string extras under whatever key the sender chose.
 *
 * No host allowlist is applied. A push comes from this app's own FCM project
 * and is authenticated at the sender; running it through the allowlist meant
 * for config-endpoint answers dropped every campaign link whose host was not
 * spelled out in `gray.allowedHosts`, and the launcher then routed the tap as
 * an ordinary start — which is the "cold start onto the page I was on before
 * the push" report.
 *
 * `intent.data` is deliberately not read. An AppsFlyer OneLink open is a VIEW
 * intent with the link in exactly that field and no extras, so reading it here
 * would turn every deep link into a fake push and skip attribution.
 */
internal object BastionTap {

    private const val TAG = "BastionTap"

    /** Our own extras. Must match the constants on Gate / Shell. */
    const val EXTRA_FROM_PUSH = "from_push"
    const val EXTRA_PUSH_URL  = "push_url"

    /** Names a sender may use for the destination in a `data` payload. */
    private val KNOWN_KEYS = listOf(
        "url", "link", "deeplink", "deep_link", "target_url", "click_url", "click_action",
        "gcm.notification.url", "gcm.notification.link", "gcm.notification.click_action",
    )

    /**
     * Keys that legitimately hold a URL which is *not* the destination.
     * The big-picture image is the one that actually bites: a valid https
     * string sitting in the same bundle that a blind scan would open.
     */
    private val NOT_A_DESTINATION = listOf("image", "icon", "picture", "sound", "avatar")

    /** The destination a tap carries, or null when there is none. */
    fun urlFrom(intent: Intent): String? {
        val extras = intent.extras ?: return null

        // Shape 1 — our own intent. Unambiguous, so it is read unconditionally.
        stringExtra(extras, EXTRA_PUSH_URL)?.takeIf(::isWebUrl)?.let { return it }

        // Shape 2 — the Firebase SDK drew the notification and flattened the
        // data payload into the launcher intent. Only then is it safe to guess
        // at key names: `stream_url`, which this app puts on its own internal
        // intents, would otherwise match the url/link scan below and make an
        // ordinary navigation look like a push.
        if (!fromFirebase(extras)) return null

        for (key in KNOWN_KEYS) {
            val value = stringExtra(extras, key)
            if (isWebUrl(value)) return value
        }
        for (key in extras.keySet()) {
            if (!key.contains("url", true) && !key.contains("link", true)) continue
            if (NOT_A_DESTINATION.any { key.contains(it, true) }) continue
            val value = stringExtra(extras, key)
            if (isWebUrl(value)) return value
        }

        Trace.w(TAG, "push tap carries no URL — extras: ${extras.keySet().joinToString()}")
        return null
    }

    private fun fromFirebase(extras: Bundle): Boolean =
        extras.keySet().any {
            it == "from" || it.startsWith("google.") || it.startsWith("gcm.")
        }

    private fun stringExtra(extras: Bundle, key: String): String? =
        runCatching { extras.getString(key) }.getOrNull()?.trim()

    private fun isWebUrl(value: String?): Boolean =
        !value.isNullOrBlank() &&
            (value.startsWith("http://", true) || value.startsWith("https://", true))
}
