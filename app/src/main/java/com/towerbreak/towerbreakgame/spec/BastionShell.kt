package com.towerbreak.towerbreakgame.spec

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewClient.ERROR_UNSUPPORTED_SCHEME
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.towerbreak.towerbreakgame.BuildConfig
import com.towerbreak.towerbreakgame.BastionImmersive
import com.towerbreak.towerbreakgame.cache.BastionSpec
import com.towerbreak.towerbreakgame.trail.Trace
import com.towerbreak.towerbreakgame.trail.UserAgent
import com.towerbreak.towerbreakgame.ignite.BastionBus
import com.towerbreak.towerbreakgame.ignite.BastionTap
import com.towerbreak.towerbreakgame.link.BastionVault
import com.towerbreak.towerbreakgame.pulse.BastionLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen WebView shell.
 *
 *  - Black background everywhere (no Android system flash on load / page exit).
 *  - Safe-area paddings: top inset in portrait, left+right insets in landscape
 *    (handles notch / cutout cameras).
 *  - Instant BastionOffline navigation on connectivity loss — no DNS probe.
 *  - Keyboard handled by [BastionPan] (the view slides, it never resizes) plus the
 *    safe-area CSS kill injection.
 *  - A loading cover over redirect hops and failed loads, so the user only ever sees
 *    a finished page — never an intermediate hop or the WebView's own error page.
 *  - Cold + warm push URL routing through Intent extras / onNewIntent.
 *  - User-Agent ends with "appid/<bundleId> appname/<AppName>".
 */
class BastionShell : AppCompatActivity() {

    private lateinit var wv: WebView
    private lateinit var container: FrameLayout
    private lateinit var vault: BastionVault
    private lateinit var wire: BastionLink
    private lateinit var keyboard: BastionPan
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Back-navigation callback. Enabled only when the WebView has history
     * to go back to, so the OS default (do-nothing on task root) kicks in
     * when the user is on the first page — back must not close the app.
     */
    private lateinit var backCallback: OnBackPressedCallback

    /** Last main-frame URL that actually settled. What a renderer recovery reloads. */
    private var lastMainFrameUrl: String? = null

    /**
     * Deepest main-frame URL seen, settled or not. A redirect loop is resumed
     * from here rather than from the last settled page — restarting the chain
     * from its entry point only walks into the same loop again (pitfalls #30).
     */
    private var deepestHop: String? = null

    private var redirectRetries = 0
    /** One fallback to the configured entry point per settled page. */
    private var entryPointRetried = false
    private var rendererRecoveries = 0

    /** A failed load still reaches onPageFinished; without this it resets the budget. */
    private var loadFailed = false
    /** True once one page of this session has rendered — gates the cover. */
    private var firstPageSettled = false
    /** Keeps the cover raised across the reload a retry queues up. */
    private var retryPending = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val fc = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        fc.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
                ?: arrayOf()
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = BastionVault(applicationContext)
        wire  = BastionLink(applicationContext)
        BastionBus.shellAlive = true

        // Background stays black at all times — windowBackground in the theme is black,
        // and we keep the root view black too.
        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            fitsSystemWindows = false
        }
        setContentView(container)
        applyInsets()

        keyboard = BastionPan(window.decorView, vault)
        keyboard.install()
        recreateWebView()

        hideSystemUi()
        enableNotchCutout()

        // Always-enabled callback: the system never sees the back press, so it
        // can never close the Activity. On the first page navigateBack() finds
        // no prior entry and silently does nothing — the user stays put. On
        // subsequent pages it steps through real history, skipping about:blank.
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        // Choose the initial URL: push (tap extras / cold stash) > extra > saved.
        val pushFromIntent = BastionTap.urlFrom(intent)
        val coldPush = vault.consumeColdPushUrl()?.takeIf { it.isNotBlank() }
        val initial  = pushFromIntent
            ?: coldPush
            ?: intent.getStringExtra(EXTRA_STREAM_URL)
            ?: vault.destinationUrl

        if (initial.isNullOrBlank()) {
            Trace.w(TAG, "No URL to load — finishing")
            finish(); return
        }
        if (pushFromIntent != null || coldPush != null) {
            vault.coldPushUrl = null
            pushLandedAtMs = SystemClock.elapsedRealtime()
        }
        Trace.i(TAG, "loading initial URL (push=${pushFromIntent != null}, cold=${coldPush != null})")
        // The cover goes up before the engine is handed anything. onPageStarted
        // does not fire until Chromium commits to a navigation, which is several
        // frames later — and the WebView draws black until then, which is the
        // black screen reported on cold and push launches.
        raiseCover()
        wv.loadUrl(initial)

        // Connectivity monitoring — react instantly on OS callback.
        scope.launch {
            wire.connectivityFlow.collect { online ->
                if (!online) {
                    Trace.i(TAG, "Connectivity lost (callback) → BastionOffline")
                    goOffline()
                }
            }
        }

        // Heartbeat — covers the case where the page is already loaded and the user
        // turns off the internet: no WebView request fails, so we actively probe.
        scope.launch {
            while (true) {
                delay(BastionSpec.heartbeatMs)
                if (navigatedOffline) continue
                if (!wire.isConnected()) {
                    Trace.i(TAG, "Heartbeat: no network → BastionOffline")
                    goOffline()
                }
            }
        }

        scope.launch {
            delay(BastionSpec.safeAreaDelayMs)
            injectSafeAreaKill()
        }
    }

    /** Builds the WebView, puts it in the container and hooks everything to it. */
    @SuppressLint("SetJavaScriptEnabled")
    private fun recreateWebView() {
        wv = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                userAgentString = buildUserAgent()
                // Performance + compatibility.
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                loadsImagesAutomatically = true
                blockNetworkImage = false
                // Popups stay in this view. Asking for real second windows is what
                // makes the WebView demand a host for them and throw when it cannot
                // get one ("Parent WebView cannot host its own popup window").
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = true
            }
            setBackgroundColor(Color.BLACK)
            isHorizontalScrollBarEnabled = false
            isVerticalScrollBarEnabled = false
        }
        container.addView(
            wv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
        wv.webViewClient   = buildClient()
        wv.webChromeClient = buildChromeClient()
        keyboard.bind(wv)
    }

    // ── Loading cover ───────────────────────────────────────────────────

    private var cover: View? = null
    private var coverJob: Job? = null

    /**
     * Hides the empty view behind a scrim and a spinner while the session's
     * **first** page resolves. Nothing else earns a cover: every later
     * navigation, including every hop of an affiliate redirect chain, resolves
     * behind the page the user is already reading, so they see the destination
     * site appear rather than a loading screen sitting between them and it.
     *
     * Note what this is NOT: a snapshot of the view. `WebView.draw` into a software
     * canvas on a hardware-accelerated view yields solid black, which is precisely the
     * "black screen between redirects" this replaced.
     *
     */
    private fun raiseCover() {
        coverJob?.cancel()
        coverJob = null
        val existing = cover
        if (existing != null) {
            existing.animate().cancel()
            existing.alpha = 1f
            return
        }
        val fresh = FrameLayout(this).apply {
            // 0xFF0B0B0F — fully opaque near-black with a warm undertone.
            // Fully opaque is essential: a translucent scrim over the engine's
            // own error page (Chromium "little Android robot") still lets the
            // error art through, which is what the user complained about.
            setBackgroundColor(COVER_BG)
            isClickable = true
            val spinner = android.widget.ProgressBar(this@BastionShell).apply {
                isIndeterminate = true
                // Gold accent matches the game's primary brand colour.
                indeterminateTintList =
                    android.content.res.ColorStateList.valueOf(COVER_SPINNER_COLOR)
                val sizePx = (56 * resources.displayMetrics.density + 0.5f).toInt()
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx, android.view.Gravity.CENTER)
            }
            addView(spinner)
        }
        cover = fresh
        container.addView(
            fresh,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        // A page that never reports back must not hold the screen for good.
        scope.launch {
            delay(COVER_MAX_MS)
            if (cover === fresh) {
                Trace.w(TAG, "Loading cover timed out")
                dropCover(0L)
            }
        }
    }

    /**
     * @param after grace before the page is handed back. A redirect hop finishes and
     *   starts the next load within a frame or two, and this is what keeps the cover
     *   from blinking off and on between them.
     *
     * Must not run while a redirect retry is in flight: a failed load still reaches
     * [onPageFinished] and [onProgressChanged](100), and without this guard those
     * callbacks would drop the cover mid-chain, exposing the Chromium error page
     * for the full 60 ms before the next postLoad fires.
     */
    private fun dropCover(after: Long = COVER_LINGER_MS) {
        if (retryPending) return
        val current = cover ?: return
        coverJob?.cancel()
        coverJob = scope.launch {
            delay(after)
            if (cover !== current) return@launch
            cover = null
            current.animate().alpha(0f).setDuration(150L).withEndAction {
                container.removeView(current)
            }.start()
        }
    }

    // ── WebView clients ────────────────────────────────────────────────

    private var pageStartMs = 0L

    private fun buildClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val u = req.url.toString()
            val scheme = u.substringBefore(':').lowercase()
            return when {
                scheme in WEB_SCHEMES -> {
                    if (req.isForMainFrame) deepestHop = u
                    false  // load inside this WebView
                }
                scheme == "intent" -> { openIntentUri(u); true }
                // Everything else is an app link: banks, wallets, messengers, stores.
                // Handing it to the WebView would only produce ERR_UNKNOWN_URL_SCHEME,
                // and the list of schemes worth knowing about has no end.
                else -> { openExternally(u); true }
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            pageStartMs = System.currentTimeMillis()
            loadFailed = false
            retryPending = false
            keyboard.forget()
            if (url != BLANK) deepestHop = url
            // The cover is NOT raised here. It went up in onCreate for the
            // session's first page, and raising it per navigation is the scrim
            // that flashes on every redirect hop (pitfalls #33).
            Trace.i(TAG, "onPageStarted")
        }

        override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
            if (!req.isForMainFrame) return
            loadFailed = true
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) err.errorCode else -1
            val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) err.description.toString() else ""
            Trace.w(TAG, "main-frame error $code on ${req.url.host}")

            // A custom scheme reaching this point was already handed to the system;
            // the page behind it is still fine, so give it straight back.
            if (code == ERROR_UNSUPPORTED_SCHEME) {
                dropCover(0L)
                return
            }

            val isLoop = code == -9 || code == -1007 ||
                    desc.contains("too_many", ignoreCase = true)
            if (isLoop) {
                // Wipe the engine's own error page immediately — otherwise Chromium
                // paints its "little Android + Play offline" placeholder in the
                // 60 ms gap before the retry lands, and that is exactly the black
                // screen users report during redirect chains (pitfalls #8).
                view.stopLoading()
                view.loadUrl(BLANK)
                handleRedirectLoop(view, req.url.toString())
                return
            }

            val isNetErr = code in setOf(-2, -6, -7, -8, -11)
            if (isNetErr || !wire.isConnected()) {
                view.stopLoading()
                view.loadUrl(BLANK)
                goOffline()
                return
            }

            // Anything else — a server 5xx or an unclassified failure. The engine
            // has *some* page committed by now, and if we do nothing that page
            // is Chromium's error placeholder. Blank the WebView so the cover
            // stays over emptiness rather than over the error art, then hand off
            // to the offline screen: leaving the user under a stuck spinner is
            // worse than the retry loop offered by BastionOffline.
            view.stopLoading()
            view.loadUrl(BLANK)
            goOffline()
        }

        override fun onPageFinished(view: WebView, url: String) {
            Trace.i(TAG, "onPageFinished")
            if (loadFailed || url == BLANK) return
            val wasFirstPage = !firstPageSettled
            redirectRetries = 0
            entryPointRetried = false
            retryPending = false
            firstPageSettled = true
            lastMainFrameUrl = url
            deepestHop = url
            // The engine records every redirect hop and every retry as a real
            // back-stack entry, so the first Back would land on hop 19 of the
            // chain or on the error page a retry was launched from. Making the
            // first settled page the base entry drops all of it (pitfalls #37).
            if (wasFirstPage) view.clearHistory()
            injectSafeAreaKill()
            view.evaluateJavascript(keyboard.script, null)
            dropCover()
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: android.webkit.RenderProcessGoneDetail
        ): Boolean {
            Trace.w(TAG, "render process gone, crashed=${detail.didCrash()}")
            if (isFinishing || view !== wv) {
                runCatching { view.destroy() }
                return true
            }
            if (rendererRecoveries >= MAX_RENDERER_RECOVERIES) {
                Trace.w(TAG, "renderer recovery budget exhausted → BastionOffline")
                goOffline()
                return true
            }
            rendererRecoveries++
            replaceWebView()
            return true
        }
    }

    /**
     * ERR_TOO_MANY_REDIRECTS. Chromium gives up after 20 hops and affiliate
     * chains are routinely longer, so this is an ordinary condition rather than
     * a failure — the chain has to be resumed, not restarted.
     *
     * The cover is raised on every scheduled retry so the user sees a loading
     * screen instead of the Chromium error page during the 60 ms gap before
     * postLoad fires. [dropCover] has a symmetric guard: it will not run while
     * [retryPending] is true, so the cover stays up across [onPageFinished] and
     * [onProgressChanged](100) that a failed load still triggers.
     */
    private fun handleRedirectLoop(view: WebView, failedUrl: String) {
        if (redirectRetries < BastionSpec.redirectRetryMax) {
            redirectRetries++
            retryPending = true
            raiseCover()
            val resumeAt = deepestHop ?: failedUrl
            Trace.i(TAG, "redirect loop, resuming attempt $redirectRetries")
            postLoad(view, resumeAt)
            return
        }

        // Budget spent. The chain itself is stuck; the entry point the backend
        // named usually still resolves, and cookies picked up along the way are
        // often what the chain was missing.
        val entryPoint = vault.destinationUrl
        if (!entryPointRetried && !entryPoint.isNullOrBlank() && entryPoint != deepestHop) {
            entryPointRetried = true
            retryPending = true
            raiseCover()
            Trace.w(TAG, "redirect budget spent → retrying the configured entry point")
            postLoad(view, entryPoint)
            return
        }

        Trace.w(TAG, "redirect chain unresolvable — handing the page back")
        retryPending = false
        dropCover(0L)
    }

    /**
     * A load queued out of a WebViewClient callback. The short pause is dead
     * time in the middle of a navigation, not a delay the user can feel.
     */
    private fun postLoad(view: WebView, url: String) {
        view.postDelayed({
            if (!isFinishing && !isDestroyed) view.loadUrl(url)
        }, RETRY_PAUSE_MS)
    }

    /**
     * Builds a fresh WebView after a renderer death and puts the last good page back.
     * The dead one cannot be reused for anything, including being asked what it was
     * showing, so [lastMainFrameUrl] is what there is to go on.
     */
    private fun replaceWebView() {
        val resumeAt = lastMainFrameUrl ?: vault.destinationUrl ?: return
        val dead = wv
        container.removeView(dead)
        runCatching { dead.destroy() }
        recreateWebView()
        wv.loadUrl(resumeAt)
    }

    private fun buildChromeClient() = object : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            // Backstop for a page that reports progress but never a finished load.
            // about:blank is only ever loaded on the way out to the offline screen,
            // so its progress says nothing about the page the user is waiting for.
            //
            // The two guarded flags are the real point of this method: without
            // them, a redirect-loop retry lets the engine paint its own error
            // page to 100 % before onPageFinished fires, this callback drops the
            // cover, and the user sees the Android-robot placeholder for the
            // full 60 ms retry gap (pitfalls #8).
            if (newProgress < 100 || view.url == BLANK) return
            if (loadFailed || retryPending) return
            dropCover()
        }

        override fun onShowFileChooser(
            view: WebView, callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            fileCallback?.onReceiveValue(arrayOf())
            fileCallback = callback
            return try {
                filePicker.launch(params.createIntent())
                true
            } catch (_: Exception) {
                fileCallback = null
                false
            }
        }
    }

    // ── Links the WebView cannot take ───────────────────────────────────

    private fun openExternally(url: String) {
        val intent = runCatching {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.getOrNull() ?: return
        launchOrIgnore(intent)
    }

    /**
     * intent:// URIs name a target app and usually carry a browser_fallback_url, so
     * there are three things to try before the user is left looking at nothing.
     */
    private fun openIntentUri(url: String) {
        val parsed = runCatching {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        }.getOrNull() ?: return
        val fallback = parsed.getStringExtra("browser_fallback_url")
        parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        parsed.addCategory(Intent.CATEGORY_BROWSABLE)
        parsed.component = null
        parsed.selector = null

        if (launchOrIgnore(parsed)) return
        // The named app may be missing while some other app handles the scheme.
        parsed.`package` = null
        if (launchOrIgnore(parsed)) return
        if (!fallback.isNullOrBlank()) wv.loadUrl(fallback)
    }

    private fun launchOrIgnore(intent: Intent): Boolean =
        runCatching { startActivity(intent) }.isSuccess

    // ── Navigation ──────────────────────────────────────────────────────

    @Volatile private var navigatedOffline = false

    /** When a push URL was last handed to the WebView. Guards late re-routing. */
    private var pushLandedAtMs = 0L

    /** The notification promo gets one showing per Activity instance. */
    private var optInOffered = false

    /**
     * Step back to the nearest real page. about:blank entries are the black
     * screen users hit after error recovery / offline blanks — skip them.
     * With no meaningful prior page, stay put (never finish the activity).
     */
    private fun navigateBack() {
        val list = wv.copyBackForwardList()
        var idx = list.currentIndex - 1
        while (idx >= 0) {
            val u = list.getItemAtIndex(idx)?.url
            if (!u.isNullOrBlank() && !isBlankUrl(u)) {
                wv.goBackOrForward(idx - list.currentIndex)
                return
            }
            idx--
        }
        // Nowhere real to go — callback stays enabled but does nothing, so
        // back on the first page is a silent no-op instead of exiting the app.
    }

    private fun isBlankUrl(url: String): Boolean =
        url == BLANK || url.startsWith("about:blank")

    private fun goOffline() {
        if (navigatedOffline) return
        navigatedOffline = true
        val cur = lastMainFrameUrl ?: wv.url
        try { wv.stopLoading(); wv.loadUrl(BLANK) } catch (_: Exception) {}
        startActivity(Intent(this, BastionOffline::class.java).apply {
            if (!cur.isNullOrBlank()) putExtra(BastionOffline.EXTRA_RETURN_URL, cur)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigatedOffline = false

        // Push URL wins over the saved stream destination — otherwise a tap
        // reopens the previous page and looks like "notifications don't work".
        val pushUrl = BastionTap.urlFrom(intent)
        if (pushUrl != null) {
            Trace.i(TAG, "push → loading")
            vault.coldPushUrl = null
            pushLandedAtMs = SystemClock.elapsedRealtime()
            wv.loadUrl(pushUrl)
            return
        }

        // Anything arriving right behind a push tap (the OS re-delivering the
        // launcher intent, a screen handing control back) must not reload the
        // saved page over the link the user just opened.
        if (SystemClock.elapsedRealtime() - pushLandedAtMs < PUSH_HOLD_MS) {
            Trace.i(TAG, "onNewIntent ignored — push navigation in flight")
            return
        }

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        val current = wv.url
        val target = streamUrl ?: vault.destinationUrl
        if (!target.isNullOrBlank() &&
            (current.isNullOrBlank() || isBlankUrl(current) || current != target)) {
            Trace.i(TAG, "onNewIntent → reloading target")
            wv.loadUrl(target)
        }
    }

    // ── Insets / safe area ──────────────────────────────────────────────

    /**
     * Apply orientation-aware padding so the WebView never sits under the camera
     * notch / cutout.
     *   portrait  → top inset only
     *   landscape → left + right insets (cutout on either side)
     */
    private fun applyInsets() {
        container.setOnApplyWindowInsetsListener { v, insets ->
            val isLandscape = resources.configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                insets.displayCutout else null
            val topPad   = if (!isLandscape) (cutout?.safeInsetTop   ?: 0).coerceAtLeast(insetTop(insets))   else 0
            val leftPad  = if (isLandscape)  (cutout?.safeInsetLeft  ?: 0).coerceAtLeast(insetLeft(insets))  else 0
            val rightPad = if (isLandscape)  (cutout?.safeInsetRight ?: 0).coerceAtLeast(insetRight(insets)) else 0
            v.setPadding(leftPad, topPad, rightPad, 0)
            insets
        }
        container.requestApplyInsets()
    }

    private fun insetTop(insets: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            insets.getInsets(WindowInsets.Type.systemBars()).top else 0
    private fun insetLeft(insets: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            insets.getInsets(WindowInsets.Type.systemBars()).left else 0
    private fun insetRight(insets: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            insets.getInsets(WindowInsets.Type.systemBars()).right else 0

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        container.requestApplyInsets()
        keyboard.remeasure()
        BastionImmersive.apply(this)
    }

    override fun onResume() {
        super.onResume()
        // Belt-and-braces for the push-tap hand-off. onStart already consumes
        // the bus queue for the common case (Activity returning to foreground),
        // but a push that lands with the shell already fully resumed (window
        // still on screen, launcher intent absorbed by BastionGate.finish())
        // only fires onResume once the OS re-focuses this instance. Draining
        // the queue here as well is what makes the tapped URL actually load.
        BastionBus.consume()?.let { url ->
            Trace.i(TAG, "queued push URL (onResume) → loading")
            vault.coldPushUrl = null
            pushLandedAtMs = SystemClock.elapsedRealtime()
            runCatching { wv.loadUrl(url) }
        }
    }

    private fun hideSystemUi() = BastionImmersive.apply(this)

    private fun enableNotchCutout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    // ── JS injections ───────────────────────────────────────────────────

    /**
     * Safe-area CSS kill. The window already pads for the cutout, so a page that
     * also honours `env(safe-area-inset-*)` would leave a second empty band on
     * top of ours. Zeroing the variables removes that band.
     *
     * What it must not do is lay a finger on the page's own box model. An
     * earlier version zeroed `padding-left`, `padding-right` and `margin` on
     * `html, body, #__nuxt, #app, #root` — but sites build their gutters with
     * exactly those declarations, so the whole layout got squeezed flat against
     * both edges (pitfalls #10). Only `padding-top`, and only on the chrome
     * wrappers that are known to add a status-bar offset of their own.
     */
    private fun injectSafeAreaKill() {
        val sentinel = BuildConfig.JS_SAFE_AREA_SENTINEL
        val running  = sentinel + "R"
        wv.evaluateJavascript("""
            (function(){
              if(window.$running) return; window.$running = true;
              var CSS_ID = '$sentinel';
              var CSS_TEXT =
                ':root{' +
                  '--safe-area-inset-top:0px!important;' +
                  '--safe-area-inset-right:0px!important;' +
                  '--safe-area-inset-bottom:0px!important;' +
                  '--safe-area-inset-left:0px!important;' +
                  '--sat:0px!important;--sar:0px!important;' +
                  '--sab:0px!important;--sal:0px!important;' +
                  '--safe-top:0px!important;--safe-right:0px!important;' +
                  '--safe-bottom:0px!important;--safe-left:0px!important;' +
                '}' +
                '.gameview-mobile-header,.app-header{' +
                  'padding-top:0!important;' +
                '}';
              function apply(){
                var head = document.head || document.documentElement;
                if (!head) return;
                var m = document.querySelector('meta[name="viewport"]');
                if (m && !/viewport-fit\s*=\s*contain/i.test(m.getAttribute('content') || '')) {
                  var c = (m.getAttribute('content') || '')
                    .replace(/,?\s*viewport-fit\s*=\s*\w+/ig, '').trim();
                  m.setAttribute('content', c + (c ? ', ' : '') + 'viewport-fit=contain');
                }
                var s = document.getElementById(CSS_ID);
                if (!s) {
                  s = document.createElement('style');
                  s.id = CSS_ID;
                  head.appendChild(s);
                }
                if (s.textContent !== CSS_TEXT) s.textContent = CSS_TEXT;
                if (head.lastElementChild !== s) head.appendChild(s);
              }
              apply();
              ['pushState','replaceState'].forEach(function(fn){
                var orig = history[fn];
                history[fn] = function(){
                  var r = orig.apply(this, arguments);
                  setTimeout(apply, 80);
                  setTimeout(apply, 400);
                  return r;
                };
              });
              window.addEventListener('popstate', function(){ setTimeout(apply, 80); });
              setInterval(apply, 2500);
            })();
        """.trimIndent(), null)
    }

    // ── User agent ──────────────────────────────────────────────────────

    private fun buildUserAgent(): String = UserAgent.value

    override fun onStart() {
        super.onStart()
        navigatedOffline = false
        BastionBus.onWarmUrl = { url ->
            runOnUiThread {
                Trace.i(TAG, "BastionBus warm URL → loading")
                try { wv.loadUrl(url) } catch (_: Exception) {}
            }
        }
        BastionBus.consume()?.let { url ->
            Trace.i(TAG, "queued push URL → loading")
            vault.coldPushUrl = null
            pushLandedAtMs = SystemClock.elapsedRealtime()
            runCatching { wv.loadUrl(url) }
        }

        // The per-Activity `optInOffered` guard is what stops the promo from
        // re-appearing after 3 days when the shell stayed alive in the
        // background: onStart runs, the snooze has elapsed, but the flag from
        // the previous showing still says "already shown here". Clear it once
        // the snooze is up so the promo can come back on schedule.
        if (optInOffered && vault.shouldShowNotifScreen()) {
            optInOffered = false
        }

        try {
            maybeOfferNotifications()
        } catch (e: Exception) {
            Trace.w(TAG, "maybeOfferNotifications failed: ${e.message}")
        }
    }

    /**
     * Launcher taps resume this singleTask shell without re-entering
     * BastionGate, so a snooze that has run out would never be noticed there.
     *
     * Three guards, all of them earned: the promo must never sit on top of a
     * page the user reached from a notification (it comes back through
     * [BastionOptIn.EXTRA_OVER_SHELL] and the pushed page stays put), it must
     * not appear over the session's first load, and it gets one showing per
     * Activity so a permission dialog returning here cannot loop it.
     */
    private fun maybeOfferNotifications() {
        if (isFinishing || optInOffered || !firstPageSettled) return
        if (SystemClock.elapsedRealtime() - pushLandedAtMs < PUSH_HOLD_MS) return
        if (!vault.shouldShowNotifScreen()) return
        optInOffered = true
        Trace.i(TAG, "notif snooze elapsed → BastionOptIn")
        startActivity(
            Intent(this, BastionOptIn::class.java)
                .putExtra(BastionOptIn.EXTRA_OVER_SHELL, true)
        )
    }

    override fun onStop() {
        if (BastionBus.onWarmUrl != null) BastionBus.onWarmUrl = null
        super.onStop()
    }

    override fun onDestroy() {
        if (BastionBus.onWarmUrl != null) BastionBus.onWarmUrl = null
        BastionBus.shellAlive = false
        scope.cancel()
        try { wv.destroy() } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_PUSH_URL   = "push_url"
        /** Set when the shell is opened from a notification tap (cold or warm). */
        const val EXTRA_FROM_PUSH  = "from_push"
        private const val TAG = "BastionShell"

        /** Everything the WebView itself can take. Anything else belongs to an app. */
        private val WEB_SCHEMES =
            setOf("http", "https", "about", "data", "blob", "file", "javascript")

        private const val BLANK = "about:blank"

        /** Long enough to bridge one redirect hop, short enough not to be felt. */
        private const val COVER_LINGER_MS = 120L

        /** No page may hold the screen longer than this, finished or not. */
        private const val COVER_MAX_MS = 20_000L

        /** Renderer recoveries per Activity — beyond this we go offline. */
        private const val MAX_RENDERER_RECOVERIES = 3

        /** Dim over the empty view while the session's first page resolves. */
        private const val COVER_SCRIM = 0xB3000000.toInt()

        /** Fully-opaque loading background — near-black with a warm undertone. */
        private const val COVER_BG = 0xFF0B0B0F.toInt()

        /** Gold spinner tint, matches the game's primary brand colour. */
        private const val COVER_SPINNER_COLOR = 0xFFF2C464.toInt()

        /** Pause before a queued redirect-loop retry. Long enough to let the
         *  engine finish unwinding the failed navigation, short enough to be
         *  invisible. */
        private const val RETRY_PAUSE_MS = 60L

        /** Window after a push load in which nothing else may re-route the view. */
        private const val PUSH_HOLD_MS = 8_000L
    }
}
