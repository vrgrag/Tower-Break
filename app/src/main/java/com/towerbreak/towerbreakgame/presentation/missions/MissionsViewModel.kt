package com.towerbreak.towerbreakgame.presentation.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.model.DailyMission
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the daily mission board (the Flutter `QuestBoard` screen). */
@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val missions: DailyMissionRepository,
    private val audio: GameAudio,
) : ViewModel() {

    val board: StateFlow<List<DailyMission>> = missions.board

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        viewModelScope.launch { missions.rollOverIfStale() }
    }

    fun claim(mission: DailyMission) {
        if (!mission.done || mission.claimed) return
        audio.playCue(SoundEffect.TAP)
        viewModelScope.launch {
            val paid = missions.collect(mission)
            if (paid > 0) _toast.value = "Mission complete: +$paid FUN!"
        }
    }

    fun dismissToast() { _toast.value = null }
}
