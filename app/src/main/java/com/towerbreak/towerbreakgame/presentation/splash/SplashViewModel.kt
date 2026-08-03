package com.towerbreak.towerbreakgame.presentation.splash

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.init.AppWarmUp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the cold-start warm-up (the Flutter `_BootStageState`). Decodes art and
 * hydrates persistence behind the progress bar, but never leaves before
 * [MIN_SHOW_MS] — a splash that flickers past reads as a glitch.
 *
 * Fingerprint note: the "warm up + minimum display" logic that lived in a
 * `StatefulWidget`'s `initState` now sits in a ViewModel, so it survives config
 * changes and exposes a single [ready] flag the UI observes.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val warmUp: AppWarmUp,
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            runCatching { warmUp.run() }
            val spent = SystemClock.elapsedRealtime() - startedAt
            if (spent < MIN_SHOW_MS) delay(MIN_SHOW_MS - spent)
            _ready.value = true
        }
    }

    private companion object {
        const val MIN_SHOW_MS = 2400L
    }
}
