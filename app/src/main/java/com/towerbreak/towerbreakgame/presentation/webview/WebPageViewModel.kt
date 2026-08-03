package com.towerbreak.towerbreakgame.presentation.webview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Immutable UI state for the in-app browser. */
data class WebPageUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/**
 * Holds the WebView's load state (the Flutter `_WebPageStageState` flags).
 *
 * Fingerprint note: the loading/failed booleans that were `setState` fields in a
 * `StatefulWidget` are lifted into a ViewModel-owned [StateFlow], so the state
 * survives rotation and the composable stays a pure function of it.
 */
@HiltViewModel
class WebPageViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(WebPageUiState())
    val state: StateFlow<WebPageUiState> = _state.asStateFlow()

    fun onPageFinished() = _state.update { it.copy(loading = false) }

    fun onLoadError() = _state.update { it.copy(loading = false, failed = true) }
}
