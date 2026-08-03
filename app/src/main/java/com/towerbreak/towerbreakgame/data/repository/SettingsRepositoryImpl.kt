package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.HazardBand
import com.towerbreak.towerbreakgame.domain.model.PlayerSettings
import com.towerbreak.towerbreakgame.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
) : SettingsRepository, Hydratable {

    private val _state = MutableStateFlow(PlayerSettings())
    override val state: StateFlow<PlayerSettings> = _state.asStateFlow()

    override suspend fun hydrate() {
        _state.value = PlayerSettings(
            band = HazardBand.bySlot(prefs.readInt(PreferenceStore.Keys.riskSlot, 0)),
            stake = prefs.readInt(PreferenceStore.Keys.stake, PreferenceStore.Defaults.STAKE),
            cuesOn = prefs.readBool(PreferenceStore.Keys.cuesOn, true),
            trackOn = prefs.readBool(PreferenceStore.Keys.trackOn, true),
            buzzOn = prefs.readBool(PreferenceStore.Keys.buzzOn, true),
            trackGain = prefs.readFloat(PreferenceStore.Keys.trackGain, PreferenceStore.Defaults.TRACK_GAIN),
            cueGain = prefs.readFloat(PreferenceStore.Keys.cueGain, PreferenceStore.Defaults.CUE_GAIN),
        )
    }

    override suspend fun chooseBand(band: HazardBand) {
        prefs.writeInt(PreferenceStore.Keys.riskSlot, band.slot)
        _state.value = _state.value.copy(band = band)
    }

    override suspend fun rememberStake(stake: Int) {
        prefs.writeInt(PreferenceStore.Keys.stake, stake)
        _state.value = _state.value.copy(stake = stake)
    }

    override suspend fun toggleCues(on: Boolean) {
        prefs.writeBool(PreferenceStore.Keys.cuesOn, on)
        _state.value = _state.value.copy(cuesOn = on)
    }

    override suspend fun toggleTrack(on: Boolean) {
        prefs.writeBool(PreferenceStore.Keys.trackOn, on)
        _state.value = _state.value.copy(trackOn = on)
    }

    override suspend fun toggleBuzz(on: Boolean) {
        prefs.writeBool(PreferenceStore.Keys.buzzOn, on)
        _state.value = _state.value.copy(buzzOn = on)
    }

    override suspend fun setTrackGain(gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        prefs.writeFloat(PreferenceStore.Keys.trackGain, clamped)
        _state.value = _state.value.copy(trackGain = clamped)
    }

    override suspend fun setCueGain(gain: Float) {
        val clamped = gain.coerceIn(0f, 1f)
        prefs.writeFloat(PreferenceStore.Keys.cueGain, clamped)
        _state.value = _state.value.copy(cueGain = clamped)
    }
}
