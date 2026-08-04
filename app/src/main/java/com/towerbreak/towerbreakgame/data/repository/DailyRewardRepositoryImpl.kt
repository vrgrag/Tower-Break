package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.foundation.time.GameClock
import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.repository.DailyRewardRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The seven-day login reward (the Flutter `LoginStreak`). Coming back the next
 * day escalates the payout; skipping a day drops back to day one.
 */
@Singleton
class DailyRewardRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
    private val wallet: WalletRepository,
    private val audio: GameAudio,
    private val clock: GameClock,
) : DailyRewardRepository, Hydratable {

    private var streakLength = 0
    private var lastStamp: String? = null

    private val _length = MutableStateFlow(0)
    override val length: StateFlow<Int> = _length.asStateFlow()

    private val _nextPayout = MutableStateFlow(PAYOUTS.first())
    override val nextPayout: StateFlow<Int> = _nextPayout.asStateFlow()

    private val _collectable = MutableStateFlow(true)
    override val collectable: StateFlow<Boolean> = _collectable.asStateFlow()

    override suspend fun hydrate() {
        streakLength = prefs.readInt(PreferenceStore.Keys.streakLength, 0)
        lastStamp = prefs.readString(PreferenceStore.Keys.streakStamp)?.ifEmpty { null }
        republish()
    }

    override suspend fun collect(): Int {
        if (!isCollectable()) return 0

        val unbroken = lastStamp?.let { clock.isYesterday(it) } == true
        streakLength = if (unbroken) streakLength + 1 else 1
        val payout = PAYOUTS[(streakLength - 1) % PAYOUTS.size]
        lastStamp = clock.todayStamp()

        prefs.writeInt(PreferenceStore.Keys.streakLength, streakLength)
        prefs.writeString(PreferenceStore.Keys.streakStamp, lastStamp!!)
        wallet.credit(payout)
        audio.playCue(SoundEffect.COIN)
        republish()
        return payout
    }

    private fun isCollectable(): Boolean {
        val stamp = lastStamp ?: return true
        return !clock.isToday(stamp)
    }

    /** Zero-based index into [PAYOUTS] for the next collectable day. */
    private fun nextDayIndex(): Int {
        val stamp = lastStamp ?: return 0
        val unbroken = clock.isYesterday(stamp) || clock.isToday(stamp)
        return if (unbroken) streakLength % PAYOUTS.size else 0
    }

    private fun republish() {
        _length.value = streakLength
        _nextPayout.value = PAYOUTS[nextDayIndex()]
        _collectable.value = isCollectable()
    }

    companion object {
        /** Coins for day 1 through day 7 of the streak. */
        val PAYOUTS = listOf(100, 150, 200, 300, 450, 700, 1200)
    }
}
