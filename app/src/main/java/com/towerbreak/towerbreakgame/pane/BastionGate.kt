package com.towerbreak.towerbreakgame.pane

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.towerbreak.towerbreakgame.BuildConfig
import com.towerbreak.towerbreakgame.BastionImmersive
import com.towerbreak.towerbreakgame.BastionLoader
import com.towerbreak.towerbreakgame.presentation.MainActivity
import com.towerbreak.towerbreakgame.cache.BastionSpec
import com.towerbreak.towerbreakgame.cache.BastionResult
import com.towerbreak.towerbreakgame.trail.Trace
import com.towerbreak.towerbreakgame.trail.UrlGuard
import com.towerbreak.towerbreakgame.trail.UserAgent
import com.towerbreak.towerbreakgame.spec.BastionOptIn
import com.towerbreak.towerbreakgame.spec.BastionOffline
import com.towerbreak.towerbreakgame.spec.BastionShell
import com.towerbreak.towerbreakgame.beacon.BastionClient
import com.towerbreak.towerbreakgame.ignite.BastionBus
import com.towerbreak.towerbreakgame.link.BastionVault
import com.towerbreak.towerbreakgame.link.BastionVault.RunChannel
import com.towerbreak.towerbreakgame.pulse.BastionLink
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Entry-point router. Shows the branded loading screen while performing the
 * gray/white decision in the background. State machine — every branch below
 * is grounded in `.cursor/rules/kotlin_launch_flow.mdc`; changes need a read
 * there first.
 *
 *  UNDECIDED (first launch):
 *    * No internet → BastionOffline on the first frame. Nothing started or
 *      persisted; the offline screen relaunches this router when the link
 *      returns.
 *    * Has internet → ignite AppsFlyer → attribution + deep link → config
 *      POST → decide.
 *      ok+url         → STREAM → optional BastionOptIn → BastionShell
 *      otherwise      → the native part, and persist NATIVE only when the
 *                       endpoint really answered AND the attribution was
 *                       non-empty.
 *
 *  STREAM (was WebView last time):
 *    * No internet → BastionOffline with the saved URL.
 *    * Cold push URL → BastionShell (highest priority).
 *    * Attribution → config POST.
 *      ok+url         → BastionShell(newUrl)
 *      failure+saved  → BastionShell(savedUrl)
 *      failure+none   → BastionOffline
 *
 *  NATIVE (was the game last time):
 *    * The game, always. Once native, stay native — including if a push URL
 *      arrives for this install.
 */
class BastionGate : AppCompatActivity() {

    private lateinit var vault: BastionVault
    private lateinit var wire: BastionLink
    private var splash: BastionLoader? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = BastionVault(applicationContext)
        wire  = BastionLink(applicationContext)

        val pushUrl = pushUrlFrom(intent)

        // Warm-tap hand-off: the shell is still alive, take the user right back
        // to the page they were on and drop this splash entirely.
        if (pushUrl != null && vault.runChannel == RunChannel.STREAM &&
            BastionBus.handOver(pushUrl)
        ) {
            Trace.i(TAG, "Warm push handed to the live shell")
            finish()
            return
        }

        // NATIVE users keep their game, regardless of what a push carries. There
        // is no decision to wait on here, so this draws no loading screen of its
        // own — the game's splash is the one boot screen for this launch, and a
        // loader flashed in front of it is the "two loading screens" complaint.
        if (vault.runChannel == RunChannel.NATIVE) {
            Trace.i(TAG, "Returning NATIVE — game, no attribution work")
            BastionImmersive.apply(this)
            scope.launch { goNative() }
            return
        }

        // Installed via a link with the radio off. Straight to the no-wifi
        // screen — no splash, no bar for a decision that will not be made.
        if (pushUrl == null &&
            vault.runChannel == RunChannel.UNDECIDED &&
            !wire.isConnected()
        ) {
            Trace.i(TAG, "First run with no link → offline first frame")
            startActivity(Intent(this, BastionOffline::class.java))
            finish()
            return
        }

        val loader = BastionLoader(this, indeterminate = true) { /* never auto-completes */ }
        splash = loader
        setContentView(loader)
        BastionImmersive.apply(this)

        // Cold-start URL: STREAM channel already means WebView was the last
        // face of the app; UNDECIDED will become STREAM via route(). NATIVE
        // was handled above.
        if (pushUrl != null) {
            Trace.i(TAG, "Cold push URL received")
            vault.coldPushUrl = pushUrl
        }

        scope.launch { route() }
    }

    // ── State machine ───────────────────────────────────────────────────────

    private suspend fun route() {
        val forced = BuildConfig.DEBUG_FORCE_URL
        if (BuildConfig.DEBUG && forced.isNotBlank()) {
            Trace.w(TAG, "DEBUG: forcing stream URL")
            goGray(forced)
            return
        }

        val coldPush = vault.coldPushUrl
        if (!coldPush.isNullOrBlank() && UrlGuard.accepts(coldPush)) {
            Trace.i(TAG, "Cold push URL → STREAM directly")
            vault.coldPushUrl = null
            if (vault.runChannel == RunChannel.UNDECIDED)
                vault.runChannel = RunChannel.STREAM
            goGray(coldPush)
            return
        }

        when (vault.runChannel) {
            RunChannel.NATIVE   -> goNative()
            RunChannel.STREAM   -> handleOnlineReturn()
            RunChannel.UNDECIDED -> handleFirstLaunch()
        }
    }

    private suspend fun handleFirstLaunch() {
        if (!ensureInternet(isFirstLaunch = true)) return

        val tracker = (applicationContext as BastionApp).trackingDispatch
        tracker.ignite(this)
        tracker.retrace(this)
        val attribution = tracker.awaitAttribution(BastionSpec.attributionFirstMs)

        val result = fetchConfig(attribution)
        if (result.active && !result.destination.isNullOrBlank()) {
            vault.runChannel     = RunChannel.STREAM
            vault.destinationUrl = result.destination
            vault.urlExpiresAt   = result.expiresAt
            goGray(result.destination)
        } else {
            // A "no" sticks forever, so it has to be a real one — the endpoint
            // did answer, and it did with this install's attribution in hand.
            when {
                !result.answered ->
                    Trace.i(TAG, "endpoint unreachable → game, decision left open")
                attribution.isEmpty() ->
                    Trace.i(TAG, "no attribution behind the answer → game, decision left open")
                else -> {
                    vault.runChannel = RunChannel.NATIVE
                    Trace.i(TAG, "backend → NATIVE")
                }
            }
            goNative()
        }
    }

    private suspend fun handleOnlineReturn() {
        if (!ensureInternet(isFirstLaunch = false)) return

        val coldPush = vault.consumeColdPushUrl()
        if (!coldPush.isNullOrBlank() && UrlGuard.accepts(coldPush)) {
            goGray(coldPush)
            return
        }

        val savedUrl = if (vault.isUrlValid()) vault.destinationUrl else null

        val tracker = (applicationContext as BastionApp).trackingDispatch
        tracker.ignite(this)
        tracker.retrace(this)
        val attribution = tracker.awaitAttribution(BastionSpec.attributionReturnMs)

        val result = fetchConfig(attribution)
        when {
            result.active && !result.destination.isNullOrBlank() -> {
                vault.destinationUrl = result.destination
                vault.urlExpiresAt   = result.expiresAt
                goGray(result.destination)
            }
            !savedUrl.isNullOrBlank() -> {
                goGray(savedUrl)
            }
            else -> handOver {
                startActivity(Intent(this, BastionOffline::class.java))
                finish()
            }
        }
    }

    private suspend fun ensureInternet(isFirstLaunch: Boolean): Boolean {
        if (wire.isConnected()) return true

        // The old code left a collect on `wire.connectivityFlow` hanging past
        // the first `resume`. This variant hands ownership of a Job to the
        // suspended coroutine and cancels it on completion or cancellation.
        val gate = Channel<Boolean>(capacity = Channel.CONFLATED)
        val watcher: Job = scope.launch {
            wire.connectivityFlow.collect { ok -> gate.trySend(ok) }
        }
        val online = try {
            withTimeoutOrNull(BastionSpec.connectGraceMs) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val listener = scope.launch {
                        for (v in gate) if (v) { cont.resume(true); break }
                    }
                    cont.invokeOnCancellation { listener.cancel() }
                }
            } == true
        } finally {
            watcher.cancel()
            gate.close()
        }
        if (online) return true

        val savedUrl = if (!isFirstLaunch && vault.isUrlValid()) vault.destinationUrl else null
        startActivity(
            Intent(this, BastionOffline::class.java).apply {
                if (!savedUrl.isNullOrBlank())
                    putExtra(BastionOffline.EXTRA_RETURN_URL, savedUrl)
            }
        )
        finish()
        return false
    }

    private suspend fun fetchConfig(attribution: Map<String, Any?>): BastionResult {
        val tracker = (applicationContext as BastionApp).trackingDispatch
        val fcmToken = vault.fcmToken ?: getFcmToken()?.also { vault.fcmToken = it }

        val body = tracker.buildRequestBody(
            attributionData = attribution,
            os              = "Android",
            locale          = Locale.getDefault().toLanguageTag().replace('-', '_'),
            pushToken       = fcmToken,
            firebaseProject = BastionSpec.resolveAnalyticsProject()
        )
        return BastionClient().fetchChannel(body)
    }

    // ── Navigation ──────────────────────────────────────────────────────────

    private fun handOver(go: () -> Unit) {
        val view = splash
        if (view == null) go() else view.complete { if (!isFinishing) go() }
    }

    /**
     * Hands over to the game. The game is told whether this launch already had a
     * loading screen: both draw the same artwork with the same bar, so a splash
     * that starts over after the router's finished reads as the app booting twice.
     */
    private fun goNative() {
        val alreadyLoaded = splash != null
        handOver {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_CHAINED_BOOT, alreadyLoaded)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            finish()
        }
    }

    private fun goGray(url: String) = handOver {
        val target = if (vault.shouldShowNotifScreen()) BastionOptIn::class.java
                     else BastionShell::class.java
        val extra = if (target == BastionOptIn::class.java)
            BastionOptIn.EXTRA_TARGET_URL else BastionShell.EXTRA_STREAM_URL
        startActivity(
            Intent(this, target)
                .putExtra(extra, url)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private suspend fun getFcmToken(): String? =
        withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine { cont ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
                }
            }
        }

    /**
     * The URL a notification tap carried, in either shape it can arrive in.
     *
     * A data-only message reaches [BastionFcm], which builds the tap intent with
     * this class's own extras. A message that carries a `notification` block is
     * drawn by the Firebase SDK itself whenever the app is not in the
     * foreground — that path never runs our service, and the tap opens the
     * launcher with the raw `data` payload as plain string extras instead.
     * Reading only our own extras is why a pushed link was dropped and the
     * shell reopened on the previously saved page (pitfalls #32).
     */
    private fun pushUrlFrom(intent: Intent): String? {
        val own = if (intent.getBooleanExtra(EXTRA_FROM_PUSH, false))
            intent.getStringExtra(EXTRA_PUSH_URL) else null
        val raw = intent.getStringExtra(FCM_KEY_URL) ?: intent.getStringExtra(FCM_KEY_LINK)
        return (own ?: raw)?.trim()?.takeIf { it.isNotBlank() && UrlGuard.accepts(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val pushUrl = pushUrlFrom(intent)

        if (!pushUrl.isNullOrBlank()) {
            when (vault.runChannel) {
                RunChannel.NATIVE -> {
                    Trace.i(TAG, "Push tap while NATIVE — game stays open")
                    return
                }
                RunChannel.STREAM -> {
                    if (BastionBus.handOver(pushUrl)) {
                        finish()
                        return
                    }
                    val dest = vault.destinationUrl?.takeIf { vault.isUrlValid() }
                    startActivity(
                        Intent(this, BastionShell::class.java)
                            .putExtra(BastionShell.EXTRA_STREAM_URL, dest ?: pushUrl)
                            .putExtra(BastionShell.EXTRA_PUSH_URL, pushUrl)
                            .putExtra(BastionShell.EXTRA_PUSH_WARM, true)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
                RunChannel.UNDECIDED -> vault.coldPushUrl = pushUrl
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    /** Present the current UA to callers who need to log it. */
    fun currentUserAgent(): String = UserAgent.value

    companion object {
        private const val TAG = "BastionGate"
        const val EXTRA_FROM_PUSH = "from_push"
        const val EXTRA_PUSH_URL  = "push_url"

        /** Payload keys BastionFcm reads, and the ones the SDK forwards verbatim. */
        private const val FCM_KEY_URL  = "url"
        private const val FCM_KEY_LINK = "link"
    }
}
