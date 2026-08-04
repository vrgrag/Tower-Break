package com.towerbreak.towerbreakgame.spec

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.towerbreak.towerbreakgame.R
import com.towerbreak.towerbreakgame.pane.BastionGate
import com.towerbreak.towerbreakgame.pulse.BastionLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * No-internet screen. Branded background PNG (portrait / landscape) with a RETRY button.
 * On retry, checks real connectivity and returns to BastionGate or BastionShell.
 */
class BastionOffline : AppCompatActivity() {

    private lateinit var wire: BastionLink
    private val scope = CoroutineScope(Dispatchers.Main)
    private var retryBtn: TextView? = null
    private var returnUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wire = BastionLink(applicationContext)
        returnUrl = intent.getStringExtra(EXTRA_RETURN_URL)

        val isLandscape = resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val bgRes = if (isLandscape) R.drawable.tb_nowifi_landscape
                    else R.drawable.tb_nowifi_portrait

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        val bg = ImageView(this).apply {
            setImageResource(bgRes)
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(bg)

        // Same contrast floor as the opt-in screen: the sunset art is bright at
        // the bottom edge and RETRY has to stay legible over it.
        root.addView(View(this).apply {
            setBackgroundResource(R.drawable.tb_scrim_bottom)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(if (isLandscape) 190 else 300),
                Gravity.BOTTOM,
            )
        })

        val btn = buildRetryButton()
        retryBtn = btn
        btn.setOnClickListener { tryRetry() }

        // Horizontally centered on the full window — no cutout/safe-area padding
        // (landscape notches must not shift RETRY relative to the art plate).
        val lp = FrameLayout.LayoutParams(
            dpToPx(200),
            dpToPx(52),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        )
        lp.bottomMargin = dpToPx(if (isLandscape) 34 else 52)
        lp.marginStart = 0
        lp.marginEnd = 0
        btn.layoutParams = lp
        root.addView(btn)

        setContentView(root)
        com.towerbreak.towerbreakgame.BastionImmersive.apply(this)

        scope.launch {
            wire.connectivityFlow.collect { online ->
                if (online) tryRetry()
            }
        }
    }

    private fun tryRetry() {
        retryBtn?.text = "Connecting..."
        retryBtn?.isEnabled = false
        scope.launch {
            val ok = wire.hasRealInternet()
            if (ok) {
                val next = if (!returnUrl.isNullOrBlank()) {
                    Intent(this@BastionOffline, BastionShell::class.java)
                        .putExtra(BastionShell.EXTRA_STREAM_URL, returnUrl)
                        .setFlags(FLAG_ACTIVITY_CLEAR_TOP)
                } else {
                    // With no page to go back to, this is a first launch that began
                    // offline: the router runs its decision from the top, which is
                    // the first time AppsFlyer is asked anything. Patching state
                    // here instead would settle the install as organic.
                    Intent(this@BastionOffline, BastionGate::class.java)
                        .setFlags(FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(next)
                finish()
            } else {
                retryBtn?.text = "RETRY"
                retryBtn?.isEnabled = true
            }
        }
    }

    private fun buildRetryButton(): TextView = TextView(this).apply {
        text = "RETRY"
        textSize = 17f
        gravity = Gravity.CENTER
        setTypeface(null, android.graphics.Typeface.BOLD)
        setBackgroundResource(R.drawable.tb_btn_accept)
        setTextColor(Color.parseColor("#1A0E08"))
        setPadding(0, 0, 0, 0)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density + 0.5f).toInt()

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RETURN_URL = "offline_return_url"
    }
}
