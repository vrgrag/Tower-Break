package com.towerbreak.towerbreakgame.domain.repository

import com.towerbreak.towerbreakgame.domain.model.CosmeticsState
import com.towerbreak.towerbreakgame.domain.model.HazardBand
import com.towerbreak.towerbreakgame.domain.model.MilestoneState
import com.towerbreak.towerbreakgame.domain.model.PlayerSettings
import com.towerbreak.towerbreakgame.domain.model.ProgressionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contracts for every persisted player store. Splitting the old monolithic
 * `PlayerProfile` into focused interfaces lets each screen depend only on what it
 * reads, and lets Hilt bind a fake for tests.
 *
 * All observable state is exposed as [StateFlow]; every mutation is a suspend
 * function that writes through to DataStore before it returns.
 */

/** The player's FUN coin balance (the Flutter `CoinPurse`). */
interface WalletRepository {
    val balance: StateFlow<Int>
    fun canAfford(amount: Int): Boolean
    suspend fun credit(amount: Int)

    /** Returns false and changes nothing when the balance cannot cover [amount]. */
    suspend fun debit(amount: Int): Boolean
    suspend fun replace(amount: Int)
}

/** Banked experience and everything derived from it (the Flutter `RankSheet`). */
interface ProgressionRepository {
    val state: StateFlow<ProgressionState>
    suspend fun bank(amount: Int)
}

/** Lifetime personal bests (the Flutter `RecordBook`). */
interface MilestoneRepository {
    val state: StateFlow<MilestoneState>
    suspend fun logRound(payout: Int, height: Int, streak: Int)
}

/** Owned and equipped cosmetics (the Flutter `Wardrobe`). */
interface CosmeticsRepository {
    val state: StateFlow<CosmeticsState>
    fun ownsSkin(skin: Int): Boolean
    fun ownsBackdrop(slot: Int): Boolean
    suspend fun grantSkin(skin: Int)
    suspend fun pinSkin(skin: Int)
    suspend fun grantBackdrop(slot: Int)
    suspend fun pinBackdrop(slot: Int)
}

/** Everything the player can flip, drag or dial (the Flutter `KnobBoard`). */
interface SettingsRepository {
    val state: StateFlow<PlayerSettings>
    suspend fun chooseBand(band: HazardBand)
    suspend fun rememberStake(stake: Int)
    suspend fun toggleCues(on: Boolean)
    suspend fun toggleTrack(on: Boolean)
    suspend fun toggleBuzz(on: Boolean)
    suspend fun setTrackGain(gain: Float)
    suspend fun setCueGain(gain: Float)
}
