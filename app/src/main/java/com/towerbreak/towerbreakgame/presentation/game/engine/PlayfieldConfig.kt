package com.towerbreak.towerbreakgame.presentation.game.engine

/**
 * Static tuning for the playfield (the Flutter `WorldMetrics`).
 *
 * The simulation thinks in abstract *units*; the renderer stretches [WORLD_SPAN]
 * units across whatever pixel width it is handed, which keeps the layout
 * resolution independent. Y grows downward while the tower grows upward — the
 * mental model of a crane hanging from above.
 */
object PlayfieldConfig {
    const val WORLD_SPAN = 12.0

    const val BLOCK_SPAN = 4.7
    const val BLOCK_ASPECT_FLOOR = 0.72 // height / width
    const val BLOCK_ASPECT_CEIL = 1.12

    const val PEDESTAL_SPAN = 5.3
    const val PEDESTAL_RISE = 4.6

    const val GANTRY_SWEEP = 2.05
    const val HANG_CLEARANCE = 1.15

    const val HOOK_LIFT_ABOVE_LENS = 7.2
    const val HOOK_RISE = 4.4

    const val FALL_ACCEL = 70.0
    const val FALL_LAUNCH_SPEED = 6.0

    const val LENS_LEAD = 1.6
    const val LENS_EASE = 6.0

    const val GROUND_LINE = 7.6
    val pedestalCrown: Double get() = GROUND_LINE - PEDESTAL_RISE

    const val TUMBLE_DEPTH = 16.0

    /** Largest simulation step we integrate, so a stalled frame cannot teleport. */
    const val MAX_STEP = 1.0 / 30.0
}

/**
 * Read-only render heights per block skin. Kept as its own interface so the pure
 * engine can ask "how tall is this skin?" without depending on Android bitmaps —
 * the render layer supplies the concrete implementation.
 */
interface BlockMetrics {
    /** Render height, in world units, of block [skin]. */
    fun blockRise(skin: Int): Double
}
