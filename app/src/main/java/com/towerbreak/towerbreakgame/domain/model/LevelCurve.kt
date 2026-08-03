package com.towerbreak.towerbreakgame.domain.model

/**
 * Pure maths for the experience curve (the Flutter `RankLadder`). Rank 1 starts
 * at zero experience and each rung costs a little more than the last.
 *
 * Fingerprint note: kept as a stateless `object` of pure functions — this is
 * genuinely a value-free helper, so an interface/DI wrapper would be ceremony.
 */
object LevelCurve {
    const val TOP_RANK = 60

    /** Experience for the single step from [rank] to rank + 1. */
    private fun stepCost(rank: Int): Int = 60 + (rank - 1) * 45

    /** Cumulative experience needed to *reach* [rank] (which starts at 1). */
    fun thresholdFor(rank: Int): Int {
        var total = 0
        for (rung in 1 until rank) total += stepCost(rung)
        return total
    }

    fun rankFor(experience: Int): Int {
        var rank = 1
        while (rank < TOP_RANK && experience >= thresholdFor(rank + 1)) rank++
        return rank
    }

    fun intoRank(experience: Int): Int = experience - thresholdFor(rankFor(experience))

    fun rankSpan(experience: Int): Int {
        val rank = rankFor(experience)
        if (rank >= TOP_RANK) return 1
        return thresholdFor(rank + 1) - thresholdFor(rank)
    }

    fun fractionThroughRank(experience: Int): Float {
        val span = rankSpan(experience)
        if (span <= 0) return 1f
        return (intoRank(experience).toFloat() / span).coerceIn(0f, 1f)
    }

    /** Coins handed out for reaching [rank]. */
    fun bounty(rank: Int): Int = 100 + rank * 50

    /** Block skins gifted on reaching [rank]. */
    fun giftedSkinsAt(rank: Int): List<Int> = when (rank) {
        2 -> listOf(2)
        4 -> listOf(3)
        7 -> listOf(4)
        else -> emptyList()
    }
}
