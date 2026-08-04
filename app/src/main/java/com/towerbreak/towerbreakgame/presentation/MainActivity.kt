package com.towerbreak.towerbreakgame.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.presentation.common.theme.TowerBreakTheme
import com.towerbreak.towerbreakgame.presentation.navigation.TowerNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single host activity. All screens are Compose destinations under
 * [TowerNavHost]. Fingerprint note: replaces Flutter's `runApp(AppScope(...))`;
 * the app-lifecycle audio pausing that `AudioDesk` did via
 * `WidgetsBindingObserver` is here a plain [DefaultLifecycleObserver].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var audio: GameAudio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = audio.onForeground(true)
            override fun onStop(owner: LifecycleOwner) = audio.onForeground(false)
        })

        val chainedBoot = intent?.getBooleanExtra(EXTRA_CHAINED_BOOT, false) == true

        setContent {
            TowerBreakTheme {
                TowerNavHost(chainedBoot = chainedBoot)
            }
        }
    }

    companion object {
        /**
         * Set by the launcher when it already showed a loading screen for this
         * launch, so the splash does not run a second progress bar over the same
         * artwork.
         */
        const val EXTRA_CHAINED_BOOT = "chained_boot"
    }
}
