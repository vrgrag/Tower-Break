package com.towerbreak.towerbreakgame.domain.model

/** Reward tuning shared by the engine and the settle use cases. */
object GameTuning {
    /** Experience granted per surviving storey and per successful cash-out. */
    const val XP_PER_STOREY = 6
    const val XP_PER_CASH_OUT = 14

    const val STAKE_FLOOR = 10
    const val STAKE_CEIL = 100_000
    val STAKE_PRESETS = listOf(50, 100, 250, 500)

    const val STARTING_BALANCE = 1000
}
