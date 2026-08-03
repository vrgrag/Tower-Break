package com.towerbreak.towerbreakgame.domain.model

import androidx.compose.ui.graphics.Color
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import kotlin.math.max
import kotlin.random.Random

/**
 * One rung of the risk/reward dial (the Flutter `RiskTier`).
 *
 * A steeper band swings the gantry faster, is less likely to let a storey hold,
 * and rolls a wider (and on average larger) multiplier for the storeys that do
 * survive.
 *
 * Fingerprint note: modelled as an `enum class` carrying tuning data rather than
 * the Dart pattern of `static const` instances on a plain class. The persisted
 * [slot] equals the ordinal, so declaration order stays frozen — same contract,
 * different mechanism.
 */
enum class HazardBand(
    val slot: Int,
    val label: String,
    val tint: Color,
    /** Seconds for the gantry to cross from one extreme to the other at storey 0. */
    private val baseSwing: Double,
    private val swingGainPerStorey: Double,
    private val swingFloor: Double,
    /** Chance in 0..1 that a released block holds — the whole game of chance. */
    val holdChance: Double,
    private val rollLow: Double,
    private val rollHigh: Double,
) {
    STEADY(0, "Easy", BrandPalette.Moss, 1.55, 0.03, 0.75, 0.88, 0.85, 1.45),
    BRISK(1, "Normal", BrandPalette.Azure, 1.2, 0.04, 0.60, 0.775, 0.70, 1.95),
    FIERCE(2, "Hard", BrandPalette.Caution, 0.92, 0.05, 0.48, 0.625, 0.60, 2.70),
    RECKLESS(3, "Insane", BrandPalette.Alarm, 0.66, 0.06, 0.38, 0.485, 0.50, 3.80);

    /** A fresh per-storey multiplier, rounded to two decimals. */
    fun rollMultiplier(dice: Random): Double {
        val raw = rollLow + dice.nextDouble() * (rollHigh - rollLow)
        return Math.round(raw * 100).toDouble() / 100.0
    }

    /** Gantry half-period once [storey] floors stand, clamped for readability. */
    fun swingPeriodAt(storey: Int): Double =
        max(swingFloor, baseSwing - storey * swingGainPerStorey)

    companion object {
        fun bySlot(slot: Int): HazardBand =
            entries.getOrElse(slot.coerceIn(0, entries.lastIndex)) { STEADY }
    }
}
