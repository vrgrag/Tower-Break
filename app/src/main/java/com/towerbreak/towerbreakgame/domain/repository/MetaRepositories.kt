package com.towerbreak.towerbreakgame.domain.repository

import com.towerbreak.towerbreakgame.domain.model.DailyMission
import com.towerbreak.towerbreakgame.domain.model.LeaderboardMetric
import com.towerbreak.towerbreakgame.domain.model.LeaderboardRow
import kotlinx.coroutines.flow.StateFlow

/** A board of three missions per calendar day (the Flutter `QuestBoard`). */
interface DailyMissionRepository {
    val board: StateFlow<List<DailyMission>>
    val claimableCount: StateFlow<Int>

    /** Rolls the board over if the day has turned. Cheap to call on every entry. */
    suspend fun rollOverIfStale()

    // Gameplay signals.
    suspend fun onStoreySeated()
    suspend fun onStoreyReached(storey: Int)
    suspend fun onCashOut()
    suspend fun onRoundLost()

    /** Pays out a finished mission; returns coins granted or 0. */
    suspend fun collect(mission: DailyMission): Int
}

/** An offline weekly board of invented rivals (the Flutter `RivalBoard`). */
interface LeaderboardRepository {
    /** Bumps solely when a new weekly best is set. */
    val revision: StateFlow<Int>

    suspend fun logRound(height: Int, payout: Int, streak: Int)
    fun best(metric: LeaderboardMetric): Int
    fun standings(metric: LeaderboardMetric): List<LeaderboardRow>
    fun placeOf(metric: LeaderboardMetric): Int
    fun daysLeftInSeason(): Int
}

/** The seven-day login reward ladder (the Flutter `LoginStreak`). */
interface DailyRewardRepository {
    val length: StateFlow<Int>
    val nextPayout: StateFlow<Int>
    val collectable: StateFlow<Boolean>

    /** Grants today's reward; returns coins paid or 0 if already collected. */
    suspend fun collect(): Int
}
