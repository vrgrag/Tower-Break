package com.towerbreak.towerbreakgame.presentation.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.MusicBed
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.model.PlayerSettings
import com.towerbreak.towerbreakgame.domain.model.ProgressionState
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import com.towerbreak.towerbreakgame.domain.repository.DailyRewardRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.SettingsRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the main menu (a slimmed equivalent of the Flutter `HubStage`). Owns the
 * hub music bed and the daily-bonus claim; the shop/quests/leaderboard screens
 * from the original follow this exact pattern and plug into the same repos.
 */
@HiltViewModel
class HubViewModel @Inject constructor(
    wallet: WalletRepository,
    progression: ProgressionRepository,
    private val dailyReward: DailyRewardRepository,
    private val settings: SettingsRepository,
    private val missions: DailyMissionRepository,
    private val audio: GameAudio,
) : ViewModel() {

    val balance: StateFlow<Int> = wallet.balance
    val progressionState: StateFlow<ProgressionState> = progression.state
    val rank: StateFlow<Int> = progression.state
        .map { it.rank }
        .stateIn(viewModelScope, SharingStarted.Eagerly, progression.state.value.rank)

    val collectable: StateFlow<Boolean> = dailyReward.collectable
    val nextPayout: StateFlow<Int> = dailyReward.nextPayout
    val settingsState: StateFlow<PlayerSettings> = settings.state
    val claimableMissions: StateFlow<Int> = missions.claimableCount

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        audio.playBed(MusicBed.HUB)
        viewModelScope.launch { missions.rollOverIfStale() }
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            val paid = dailyReward.collect()
            _toast.value = if (paid > 0) "Daily bonus: +$paid FUN!" else "Come back tomorrow!"
        }
    }

    fun toggleCues() {
        audio.playCue(SoundEffect.TAP)
        viewModelScope.launch { settings.toggleCues(!settings.state.value.cuesOn) }
    }

    fun toggleTrack() {
        viewModelScope.launch { settings.toggleTrack(!settings.state.value.trackOn) }
    }

    fun toggleBuzz() {
        audio.playCue(SoundEffect.TAP)
        viewModelScope.launch { settings.toggleBuzz(!settings.state.value.buzzOn) }
    }

    fun clickCue() = audio.playCue(SoundEffect.TAP)

    fun dismissToast() { _toast.value = null }
}
