package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.core.time.GameClock
import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.LeaderboardMetric
import com.towerbreak.towerbreakgame.domain.model.LeaderboardRow
import com.towerbreak.towerbreakgame.domain.repository.LeaderboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * A weekly board that works entirely offline. With no backend, a believable
 * field of rivals is generated deterministically from the ISO week number and
 * the player's own weekly bests are slotted into it. The field reshuffles and
 * the bests reset every Monday.
 */
@Singleton
class LeaderboardRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
    private val clock: GameClock,
) : LeaderboardRepository, Hydratable {

    private var weeklyHeight = 0
    private var weeklyPayout = 0
    private var weeklyStreak = 0

    private val _revision = MutableStateFlow(0)
    override val revision: StateFlow<Int> = _revision.asStateFlow()

    override suspend fun hydrate() {
        resetIfWeekTurned()
        weeklyHeight = prefs.readInt(PreferenceStore.Keys.rivalHeight, 0)
        weeklyPayout = prefs.readInt(PreferenceStore.Keys.rivalPayout, 0)
        weeklyStreak = prefs.readInt(PreferenceStore.Keys.rivalStreak, 0)
    }

    private suspend fun resetIfWeekTurned() {
        if (prefs.readInt(PreferenceStore.Keys.rivalWeek, PreferenceStore.Defaults.RIVAL_WEEK) == clock.weekIndex()) return
        prefs.writeInt(PreferenceStore.Keys.rivalWeek, clock.weekIndex())
        prefs.writeInt(PreferenceStore.Keys.rivalHeight, 0)
        prefs.writeInt(PreferenceStore.Keys.rivalPayout, 0)
        prefs.writeInt(PreferenceStore.Keys.rivalStreak, 0)
        weeklyHeight = 0
        weeklyPayout = 0
        weeklyStreak = 0
    }

    override suspend fun logRound(height: Int, payout: Int, streak: Int) {
        resetIfWeekTurned()
        var changed = false
        if (height > weeklyHeight) {
            weeklyHeight = height; prefs.writeInt(PreferenceStore.Keys.rivalHeight, height); changed = true
        }
        if (payout > weeklyPayout) {
            weeklyPayout = payout; prefs.writeInt(PreferenceStore.Keys.rivalPayout, payout); changed = true
        }
        if (streak > weeklyStreak) {
            weeklyStreak = streak; prefs.writeInt(PreferenceStore.Keys.rivalStreak, streak); changed = true
        }
        if (changed) _revision.value += 1
    }

    override fun best(metric: LeaderboardMetric): Int = when (metric) {
        LeaderboardMetric.HEIGHT -> weeklyHeight
        LeaderboardMetric.PAYOUT -> weeklyPayout
        LeaderboardMetric.STREAK -> weeklyStreak
    }

    override fun standings(metric: LeaderboardMetric): List<LeaderboardRow> {
        val dice = Random(clock.weekIndex() * 31 + metric.ordinal)
        val names = FIELD.shuffled(dice)
        val rows = buildList {
            names.take(14).forEach { name ->
                add(LeaderboardRow(name = name, value = inventValue(metric, dice), isPlayer = false))
            }
            add(LeaderboardRow(name = "You", value = best(metric), isPlayer = true))
        }
        return rows.sortedByDescending { it.value }
    }

    override fun placeOf(metric: LeaderboardMetric): Int =
        standings(metric).indexOfFirst { it.isPlayer } + 1

    override fun daysLeftInSeason(): Int = clock.daysLeftInSeason()

    private fun inventValue(metric: LeaderboardMetric, dice: Random): Int = when (metric) {
        LeaderboardMetric.HEIGHT -> 6 + dice.nextInt(40)
        LeaderboardMetric.PAYOUT -> 500 + dice.nextInt(45000)
        LeaderboardMetric.STREAK -> 3 + dice.nextInt(28)
    }

    private companion object {
        val FIELD = listOf(
            "BrickMason", "SkyCrane", "TallTim", "NeoBuilder", "AceStacker",
            "LunaLift", "MaxFloors", "Vertigo", "IronHook", "CashKing",
            "PixelPaul", "TowerGod", "RiskyRae", "SteadyHand", "BoomBuild",
            "ClutchCleo", "HighRoller", "ZenStack", "MrCashout", "GravityKid",
        )
    }
}
