package com.towerbreak.towerbreakgame

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt application root. Fingerprint note: this is where the Flutter
 * `launchSiege()` composition root moves to — but instead of hand-assembling a
 * `ServiceHub`, Hilt builds the singleton graph lazily on first injection.
 */
@HiltAndroidApp
class TowerBreakApp : Application()
