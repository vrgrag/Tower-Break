package com.towerbreak.towerbreakgame.core.time

import java.time.LocalDate

/**
 * Calendar helpers for the daily-reset systems (streak, missions, weekly board).
 *
 * Fingerprint note: the Flutter stores each hand-rolled their own date-stamp
 * padding and "is yesterday" checks. Centralising them here removes that
 * duplication and makes the reset behaviour trivial to unit-test with a fake.
 */
interface GameClock {
    /** ISO date string ("2026-08-03") for the current local day. */
    fun todayStamp(): String
    fun isToday(stamp: String): Boolean
    fun isYesterday(stamp: String): Boolean

    /** Integer index of the current 7-day season since the epoch. */
    fun weekIndex(): Int

    /** Days until the Monday reset (1..7). */
    fun daysLeftInSeason(): Int
}

class SystemGameClock : GameClock {

    override fun todayStamp(): String = LocalDate.now().toString()

    override fun isToday(stamp: String): Boolean = stamp == todayStamp()

    override fun isYesterday(stamp: String): Boolean =
        stamp == LocalDate.now().minusDays(1).toString()

    override fun weekIndex(): Int =
        (System.currentTimeMillis() / WEEK_MILLIS).toInt()

    override fun daysLeftInSeason(): Int = 8 - LocalDate.now().dayOfWeek.value

    private companion object {
        const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
