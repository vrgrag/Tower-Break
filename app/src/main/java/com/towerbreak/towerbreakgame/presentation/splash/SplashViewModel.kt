package com.towerbreak.towerbreakgame.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.init.AppWarmUp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the cold-start warm-up (the Flutter `_BootStageState`): decodes art and
 * hydrates persistence, then flips [warmedUp].
 *
 * This reports readiness and nothing else. How long the splash stays on screen
 * is a presentation question — and it has two answers now, because the launcher
 * may already have shown a loading screen before the game was handed control —
 * so that decision lives in the composable instead of here.
 *
 * Fingerprint note: the "warm up + minimum display" logic that lived in a
 * `StatefulWidget`'s `initState` now sits in a ViewModel, so it survives config
 * changes and exposes a single flag the UI observes.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val warmUp: AppWarmUp,
) : ViewModel() {

    private val _warmedUp = MutableStateFlow(false)
    val warmedUp: StateFlow<Boolean> = _warmedUp.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { warmUp.run() }
            _warmedUp.value = true
        }
    }
}
