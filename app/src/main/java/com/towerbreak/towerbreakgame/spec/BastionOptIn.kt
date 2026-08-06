package com.towerbreak.towerbreakgame.spec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.towerbreak.towerbreakgame.R
import com.towerbreak.towerbreakgame.link.BastionVault

/**
 * Push-notification permission screen. Shown before the WebView if the player
 * hasn't granted or permanently declined notifications.
 *
 * Buttons: ACCEPT  /  SKIP
 * Assets: portrait/landscape branded PNG backgrounds.
 */
class BastionOptIn : AppCompatActivity() {

    private lateinit var vault: BastionVault
    private var pendingUrl: String? = null
    private var fromPush: Boolean = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        runCatching {
            if (granted) {
                vault.notifGranted = true
            } else {
                // Any OS-dialog denial is treated as permanent. Skip is the
                // soft path — if the user touches the system dialog and says
                // no, showing the promo again would only loop them back to a
                // dialog the OS will no longer show, which is the broken UX.
                vault.notifOsDenied = true
            }
        }
        proceed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = BastionVault(applicationContext)
        pendingUrl = intent.getStringExtra(EXTRA_TARGET_URL)
        fromPush = intent.getBooleanExtra(EXTRA_FROM_PUSH, false)

        val isLandscape = resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val bgRes = if (isLandscape) R.drawable.tb_notif_landscape
                    else R.drawable.tb_notif_portrait

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // Background image.
        val bg = ImageView(this).apply {
            setImageResource(bgRes)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(bg)

        // Contrast floor for the CTA row. The scene art stays bright and busy to
        // the bottom edge, so the row gets its own fade rather than relying on
        // the artwork having a calm band.
        val scrim = View(this).apply {
            setBackgroundResource(R.drawable.tb_scrim_bottom)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(if (isLandscape) 190 else 300),
                Gravity.BOTTOM,
            )
        }
        root.addView(scrim)

        // Accept/Skip live in the footer band, never against the plaque: every
        // pixel under the plate is window and balcony detail, and a row parked
        // there reads as dropped on top of the art instead of placed.
        //
        // Centered on the raw window with no inset padding — a landscape cutout
        // must not shift the pair off the artwork's centre line.
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipToPadding = false
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            )
            lp.bottomMargin = dpToPx(if (isLandscape) 34 else 52)
            layoutParams = lp
        }

        val acceptBtn = buildButton("ACCEPT", accent = true,  landscape = isLandscape)
        val skipBtn   = buildButton("SKIP",   accent = false, landscape = isLandscape)

        acceptBtn.setOnClickListener { onAccept() }
        skipBtn.setOnClickListener   { onSkip()   }

        btnRow.addView(acceptBtn)
        btnRow.addView(space(16))
        btnRow.addView(skipBtn)
        root.addView(btnRow)

        setContentView(root)
        com.towerbreak.towerbreakgame.BastionImmersive.apply(this)
        enableNotchCutout()
    }

    private fun enableNotchCutout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun onAccept() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                    vault.notifGranted = true
                    proceed()
                } else {
                    permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                vault.notifGranted = true
                proceed()
            }
        } catch (_: Exception) {
            // Never strand the user on this screen because of a storage / launcher
            // failure. Fall through to the shell — the promo can come back later.
            proceed()
        }
    }

    private fun onSkip() {
        runCatching {
            // Each SKIP snoozes the prompt for exactly 3 days.
            vault.notifSkipUntil =
                System.currentTimeMillis() / 1000 + 3L * 24 * 3600
        }
        proceed()
    }

    /**
     * Always navigates straight to BastionShell. Never loops back through the
     * router splash — the branded loader had its one showing for this launch.
     */
    private fun proceed() {
        val next = Intent(this, BastionShell::class.java).apply {
            pendingUrl?.let { url ->
                putExtra(BastionShell.EXTRA_STREAM_URL, url)
                if (fromPush) {
                    putExtra(BastionShell.EXTRA_PUSH_URL, url)
                    putExtra(BastionShell.EXTRA_FROM_PUSH, true)
                }
            }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(next)
        finish()
        if (fromPush) overridePendingTransition(0, 0)
    }

    private fun buildButton(label: String, accent: Boolean, landscape: Boolean): TextView {
        val tv = TextView(this)
        tv.text = label
        // In landscape the viewport height is smaller and the CTA row sits close
        // to the bottom edge, so a bigger label keeps it as readable as it is in
        // portrait rather than shrinking with the artwork.
        val textSp = when {
            accent && landscape  -> 20f
            accent && !landscape -> 17f
            !accent && landscape -> 19f
            else                 -> 16f
        }
        tv.textSize = textSp
        tv.gravity = Gravity.CENTER
        tv.setPadding(dpToPx(28), dpToPx(14), dpToPx(28), dpToPx(14))
        tv.setTypeface(null, android.graphics.Typeface.BOLD)
        tv.letterSpacing = 0.06f
        if (accent) {
            tv.setBackgroundResource(R.drawable.tb_btn_accept)
            tv.setTextColor(Color.parseColor("#1A0E08"))
            tv.setShadowLayer(2f, 0f, 1f, Color.parseColor("#66FFFFFF"))
        } else {
            tv.setBackgroundResource(R.drawable.tb_btn_skip)
            tv.setTextColor(Color.parseColor("#FFE8A23A"))
            tv.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }
        val widthDp  = if (landscape) 180 else 150
        val heightDp = if (landscape) 62  else 54
        val lp = LinearLayout.LayoutParams(dpToPx(widthDp), dpToPx(heightDp))
        tv.layoutParams = lp
        return tv
    }

    private fun space(dp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(dp), 1)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    companion object {
        const val EXTRA_TARGET_URL = "alert_target_url"
        const val EXTRA_FROM_PUSH  = "from_push"
    }
}
