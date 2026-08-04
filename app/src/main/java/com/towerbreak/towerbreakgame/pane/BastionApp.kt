package com.towerbreak.towerbreakgame.pane

import com.towerbreak.towerbreakgame.BuildConfig
import com.towerbreak.towerbreakgame.trail.Trace
import com.towerbreak.towerbreakgame.trail.UrlGuard
import com.towerbreak.towerbreakgame.beacon.BastionTrack
import com.towerbreak.towerbreakgame.ignite.BastionChannel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import android.app.Application

/**
 * Application entry: Hilt graph for the native game + Firebase (best-effort) +
 * AppsFlyer prime before any activity runs.
 */
@HiltAndroidApp
class BastionApp : Application() {

    lateinit var trackingDispatch: BastionTrack
        private set

    override fun onCreate() {
        super.onCreate()

        try {
            FirebaseApp.initializeApp(this)
            val fac = if (BuildConfig.DEBUG)
                DebugAppCheckProviderFactory.getInstance()
            else
                PlayIntegrityAppCheckProviderFactory.getInstance()
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(fac)
        } catch (e: Exception) {
            Trace.w(TAG, "Firebase not configured — gray flow will still try the config POST", e)
        }

        // Before anything can post: a push drawn by the Firebase SDK never runs
        // our service, so this is the only place guaranteed to have registered the
        // channel by the time such a notification arrives.
        BastionChannel.ensure(this)

        UrlGuard.warnIfMissing()

        trackingDispatch = BastionTrack(this)
        trackingDispatch.prime()
    }

    private companion object { const val TAG = "BastionApp" }
}
