package com.towerbreak.towerbreakgame.domain.model

/**
 * The three columns the weekly board can be sorted by (the Flutter
 * `RivalMetric`). Ordinal feeds the deterministic field seed, so it stays frozen.
 */
enum class LeaderboardMetric(val label: String) {
    HEIGHT("Height"),
    PAYOUT("Winnings"),
    STREAK("Streak");

    fun render(value: Int): String = if (this == HEIGHT) "$value fl" else "$value"
}

/** One row of the weekly board (the Flutter `RivalRow`). */
data class LeaderboardRow(
    val name: String,
    val value: Int,
    val isPlayer: Boolean,
)
