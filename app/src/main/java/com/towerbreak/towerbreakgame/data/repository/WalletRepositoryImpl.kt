package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
) : WalletRepository, Hydratable {

    private val _balance = MutableStateFlow(PreferenceStore.Defaults.PURSE)
    override val balance: StateFlow<Int> = _balance.asStateFlow()

    override suspend fun hydrate() {
        _balance.value = prefs.readInt(PreferenceStore.Keys.purse, PreferenceStore.Defaults.PURSE)
    }

    override fun canAfford(amount: Int): Boolean = _balance.value >= amount

    override suspend fun credit(amount: Int) {
        if (amount <= 0) return
        commit(_balance.value + amount)
    }

    override suspend fun debit(amount: Int): Boolean {
        if (amount <= 0 || _balance.value < amount) return false
        commit(_balance.value - amount)
        return true
    }

    override suspend fun replace(amount: Int) = commit(if (amount < 0) 0 else amount)

    private suspend fun commit(next: Int) {
        _balance.value = next
        prefs.writeInt(PreferenceStore.Keys.purse, next)
    }
}
