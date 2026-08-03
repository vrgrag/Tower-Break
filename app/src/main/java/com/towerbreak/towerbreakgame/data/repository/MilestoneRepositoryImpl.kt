package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.MilestoneState
import com.towerbreak.towerbreakgame.domain.repository.MilestoneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
) : MilestoneRepository, Hydratable {

    private val _state = MutableStateFlow(MilestoneState())
    override val state: StateFlow<MilestoneState> = _state.asStateFlow()

    override suspend fun hydrate() {
        _state.value = MilestoneState(
            topPayout = prefs.readInt(PreferenceStore.Keys.topPayout, 0),
            topHeight = prefs.readInt(PreferenceStore.Keys.topHeight, 0),
            topStreak = prefs.readInt(PreferenceStore.Keys.topStreak, 0),
            roundsPlayed = prefs.readInt(PreferenceStore.Keys.roundsPlayed, 0),
        )
    }

    // Every figure is a high-water mark, so we only write when a record falls.
    override suspend fun logRound(payout: Int, height: Int, streak: Int) {
        val current = _state.value
        val next = current.copy(
            roundsPlayed = current.roundsPlayed + 1,
            topPayout = maxOf(current.topPayout, payout),
            topHeight = maxOf(current.topHeight, height),
            topStreak = maxOf(current.topStreak, streak),
        )
        prefs.writeInt(PreferenceStore.Keys.roundsPlayed, next.roundsPlayed)
        if (next.topPayout != current.topPayout) prefs.writeInt(PreferenceStore.Keys.topPayout, next.topPayout)
        if (next.topHeight != current.topHeight) prefs.writeInt(PreferenceStore.Keys.topHeight, next.topHeight)
        if (next.topStreak != current.topStreak) prefs.writeInt(PreferenceStore.Keys.topStreak, next.topStreak)
        _state.value = next
    }
}
