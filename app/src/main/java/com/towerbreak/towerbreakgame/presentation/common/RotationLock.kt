package com.towerbreak.towerbreakgame.presentation.common

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** The rotations a screen allows (the Flutter `ScreenAxis`). */
enum class ScreenAxis(val orientation: Int) {
    /** Splash ships both orientations, so it spins freely. */
    ANY(ActivityInfo.SCREEN_ORIENTATION_SENSOR),

    /** Menus and playfield compose vertically, so they stay upright. */
    UPRIGHT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
}

/**
 * Applies [axis] to the host activity while the composable is on screen.
 *
 * Fingerprint note: replaces Flutter's imperative
 * `SystemChrome.setPreferredOrientations` calls sprinkled through screen
 * lifecycles with a declarative side effect scoped to composition.
 */
@Composable
fun RotationLock(axis: ScreenAxis) {
    val context = LocalContext.current
    DisposableEffect(axis) {
        val activity = context as? Activity
        activity?.requestedOrientation = axis.orientation
        onDispose { /* next screen sets its own axis */ }
    }
}
