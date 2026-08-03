package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.ProgressionState
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressionRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
) : ProgressionRepository, Hydratable {

    private val _state = MutableStateFlow(ProgressionState())
    override val state: StateFlow<ProgressionState> = _state.asStateFlow()

    override suspend fun hydrate() {
        _state.value = ProgressionState(
            experience = prefs.readInt(PreferenceStore.Keys.experience, 0),
        )
    }

    override suspend fun bank(amount: Int) {
        if (amount <= 0) return
        val next = _state.value.experience + amount
        prefs.writeInt(PreferenceStore.Keys.experience, next)
        _state.update { it.copy(experience = next) }
    }
}
