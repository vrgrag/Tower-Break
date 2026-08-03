package com.towerbreak.towerbreakgame.domain.model

/**
 * Immutable read models the repositories publish through their `StateFlow`s.
 *
 * Fingerprint note: the Flutter build spread this state across five
 * `ChangeNotifier` stores that screens read imperatively. Collapsing each into a
 * `data class` snapshot means Compose can just collect a flow and recompose —
 * the unidirectional-data-flow equivalent of those notifiers.
 */

data class ProgressionState(
    val experience: Int = 0,
) {
    val rank: Int get() = LevelCurve.rankFor(experience)
    val intoRank: Int get() = LevelCurve.intoRank(experience)
    val rankSpan: Int get() = LevelCurve.rankSpan(experience)
    val fractionThroughRank: Float get() = LevelCurve.fractionThroughRank(experience)
}

data class MilestoneState(
    val topPayout: Int = 0,
    val topHeight: Int = 0,
    val topStreak: Int = 0,
    val roundsPlayed: Int = 0,
)

data class CosmeticsState(
    val skinsOwned: List<Int> = listOf(1),
    /** 0 means "rotate through everything owned" rather than a pinned skin. */
    val pinnedSkin: Int = 0,
    val backdropsOwned: List<Int> = listOf(0),
    val pinnedBackdrop: Int = 0,
) {
    val backdrop: BackdropTheme get() = BackdropTheme.bySlot(pinnedBackdrop)
}

data class PlayerSettings(
    val band: HazardBand = HazardBand.STEADY,
    val stake: Int = 100,
    val cuesOn: Boolean = true,
    val trackOn: Boolean = true,
    val buzzOn: Boolean = true,
    val trackGain: Float = 0.55f,
    val cueGain: Float = 0.85f,
)

/**
 * What a promotion handed the player, so the HUD can celebrate it (the Flutter
 * `AscendReport`).
 */
data class PromotionResult(
    val promoted: Boolean,
    val rank: Int,
    val bounty: Int,
    val freshSkins: List<Int>,
) {
    companion object {
        val None = PromotionResult(false, 0, 0, emptyList())
    }
}
