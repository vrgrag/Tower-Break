package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.CosmeticsState
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CosmeticsRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
) : CosmeticsRepository, Hydratable {

    private val _state = MutableStateFlow(CosmeticsState())
    override val state: StateFlow<CosmeticsState> = _state.asStateFlow()

    override suspend fun hydrate() {
        _state.value = CosmeticsState(
            skinsOwned = prefs.readIds(PreferenceStore.Keys.skinsOwned, listOf(1)),
            pinnedSkin = prefs.readInt(PreferenceStore.Keys.pinnedSkin, 0),
            backdropsOwned = prefs.readIds(PreferenceStore.Keys.backdropsOwned, listOf(0)),
            pinnedBackdrop = prefs.readInt(PreferenceStore.Keys.pinnedBackdrop, 0),
        )
    }

    override fun ownsSkin(skin: Int): Boolean = skin in _state.value.skinsOwned
    override fun ownsBackdrop(slot: Int): Boolean = slot in _state.value.backdropsOwned

    override suspend fun grantSkin(skin: Int) {
        val current = _state.value
        if (skin in current.skinsOwned) return
        val next = (current.skinsOwned + skin).sorted()
        prefs.writeIds(PreferenceStore.Keys.skinsOwned, next)
        _state.value = current.copy(skinsOwned = next)
    }

    override suspend fun pinSkin(skin: Int) {
        prefs.writeInt(PreferenceStore.Keys.pinnedSkin, skin)
        _state.value = _state.value.copy(pinnedSkin = skin)
    }

    override suspend fun grantBackdrop(slot: Int) {
        val current = _state.value
        if (slot in current.backdropsOwned) return
        val next = (current.backdropsOwned + slot).sorted()
        prefs.writeIds(PreferenceStore.Keys.backdropsOwned, next)
        _state.value = current.copy(backdropsOwned = next)
    }

    override suspend fun pinBackdrop(slot: Int) {
        // Silently ignore a backdrop the player does not own yet.
        if (slot !in _state.value.backdropsOwned) return
        prefs.writeInt(PreferenceStore.Keys.pinnedBackdrop, slot)
        _state.value = _state.value.copy(pinnedBackdrop = slot)
    }
}
