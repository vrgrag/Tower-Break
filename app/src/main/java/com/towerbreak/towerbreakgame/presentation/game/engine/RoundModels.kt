package com.towerbreak.towerbreakgame.presentation.game.engine

import androidx.compose.ui.graphics.Color

/**
 * Where the active round is in its lifecycle (the Flutter `RoundPhase`).
 */
enum class RoundStage {
    /** Nothing staked yet — the bet bar is on screen. */
    BETTING,

    /** The gantry is carrying a block; waiting for a BUILD tap. */
    SWINGING,

    /** A block has been released and is animating down. */
    PLUNGING,

    /** The player banked the winnings. */
    BANKED,

    /** A storey failed to hold and the round is over. */
    COLLAPSED;

    /** True while a block hangs from the gantry, i.e. while the swing animates. */
    val carriesBlock: Boolean get() = this == BETTING || this == SWINGING

    val isOver: Boolean get() = this == BANKED || this == COLLAPSED
}

/** A block that has been seated into the tower (the Flutter `StackedBrick`). */
data class SeatedBlock(
    val skin: Int,
    val centreX: Double,
    /** World Y of the block's upper edge; the tower grows toward smaller Y. */
    val crownY: Double,
    val span: Double,
    val rise: Double,
    /** Landing wobble, decayed back to zero for a hand-stacked feel. */
    val tilt: Double = 0.0,
) {
    val centreY: Double get() = crownY + rise / 2
    val baseY: Double get() = crownY + rise
}

/** The block currently in free fall after a BUILD tap (the Flutter `PlungingBrick`). */
class FallingBlock(
    val skin: Int,
    var x: Double,
    /** Centre Y, not the upper edge. */
    var y: Double,
    var vy: Double,
    val span: Double,
    val rise: Double,
    /** Centre Y at which the block would seat onto the tower crown. */
    val seatY: Double,
    /** Decided at release: a doomed block tumbles past the tower instead of seating. */
    val doomed: Boolean,
    var vx: Double = 0.0,
    var angle: Double = 0.0,
    var spin: Double = 0.0,
)

/** A short-lived particle: dust puff, gold spark or flying coin (the Flutter `Mote`). */
class SparkParticle(
    var x: Double,
    var y: Double,
    var vx: Double,
    var vy: Double,
    var life: Double,
    var span: Double,
    val tint: Color,
    val gravity: Double = 0.0,
    val shrinks: Boolean = true,
    /** Blurred, cloud-like rendering, used for impact smoke. */
    val smoky: Boolean = false,
) {
    private val fullLife = 1.0
    val remaining: Double get() = (life / fullLife).coerceIn(0.0, 1.0)
}

/**
 * An immutable per-frame view the renderer walks (assembled by the engine at the
 * end of each step). Fingerprint note: the Flutter renderer read the live
 * `SiegeSim` through getters via `repaint: sim`; here the engine publishes a flat
 * snapshot so the Compose canvas has a single value to subscribe to.
 */
data class PlayfieldSnapshot(
    val stage: RoundStage = RoundStage.BETTING,
    val storey: Int = 0,
    val lensY: Double = PlayfieldConfig.pedestalCrown - PlayfieldConfig.LENS_LEAD,
    val cloudDrift: Double = 0.0,
    val tremor: Double = 0.0,
    val craneX: Double = 0.0,
    val carriesBlock: Boolean = true,
    val carriedSkin: Int = 1,
    val carriedRise: Double = PlayfieldConfig.BLOCK_SPAN,
    val hangCentreY: Double = 0.0,
    val seated: List<SeatedBlock> = emptyList(),
    val falling: FallingBlock? = null,
    val particles: List<SparkParticle> = emptyList(),
)

/**
 * Discrete HUD state pushed only when a player-readable value changes (the
 * Flutter `HudFrame`).
 */
data class RoundHudState(
    val stage: RoundStage = RoundStage.BETTING,
    val storey: Int = 0,
    /** Running product of every surviving storey's roll — what the payout uses. */
    val payoutMultiplier: Double = 0.0,
    /** The latest storey's own roll — what the big headline shows. */
    val storeyMultiplier: Double = 0.0,
    val potential: Int = 0,
    val stake: Int = 0,
    val canBank: Boolean = false,
    /** Newest first, for the results column. */
    val rolls: List<Double> = emptyList(),
    /** Turns true once the win/bust panel should appear, held back briefly. */
    val resultReady: Boolean = false,
)

/**
 * One-shot things that happen during a round, consumed by the ViewModel to drive
 * audio and the meta-game. Fingerprint note: replaces the Flutter callback
 * fields (`onStoreySeated`, `onCollapse`, `onBanked`) with a typed event stream —
 * the unidirectional "effects" channel of MVI.
 */
sealed interface RoundEvent {
    data object BlockReleased : RoundEvent
    data class StoreySeated(val storey: Int) : RoundEvent
    data class Collapsed(val storey: Int) : RoundEvent
    data class Banked(val storey: Int, val payout: Int) : RoundEvent
}
