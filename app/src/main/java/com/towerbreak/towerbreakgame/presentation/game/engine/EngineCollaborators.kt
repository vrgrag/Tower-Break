package com.towerbreak.towerbreakgame.presentation.game.engine

import androidx.compose.ui.graphics.Color
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The gantry's horizontal motion: a sine sweep whose period tightens as the
 * tower grows (the Flutter `CraneArm`). The sweep does not know whether a block
 * is attached — the engine decides whether to read [sweepOffset] or [parkedX].
 */
internal class GantrySweep {
    private var phase = 0.0
    private var parked = 0.0

    val sweepOffset: Double get() = sin(phase) * PlayfieldConfig.GANTRY_SWEEP
    val parkedX: Double get() = parked

    fun advance(dt: Double, halfPeriod: Double) {
        phase += dt * (PI / halfPeriod)
        if (phase > PI * 2) phase -= PI * 2
    }

    fun park(x: Double) { parked = x }
}

/**
 * The vertical lens (the Flutter `CameraRig`). It keeps the tower crown a fixed
 * distance below the screen centre and only ever climbs, so a collapse does not
 * yank the view back down.
 */
internal class ViewportTracker(crownY: Double) {
    var y: Double = restFor(crownY)
        private set

    fun snapTo(crownY: Double) { y = restFor(crownY) }

    fun ease(dt: Double, crownY: Double) {
        val wanted = min(y, restFor(crownY))
        y += (wanted - y) * (1 - exp(-dt * PlayfieldConfig.LENS_EASE))
    }

    private companion object {
        fun restFor(crownY: Double) = crownY - PlayfieldConfig.LENS_LEAD
    }
}

/**
 * Spawns and advances every particle effect (the Flutter `FxForge`). Shares the
 * engine's random source so the whole round draws from one stream.
 */
internal class ParticleFactory(private val dice: Random) {
    val particles = mutableListOf<SparkParticle>()

    fun advance(dt: Double) {
        particles.forEach { p ->
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
        }
        particles.removeAll { it.life <= 0 }
    }

    /** Soft smoke puffs bursting sideways from the base of a freshly seated block. */
    fun puffUnder(block: SeatedBlock) {
        val baseY = block.baseY
        val halfSpan = block.span / 2
        repeat(24) { i ->
            val side = if (i % 2 == 0) -1 else 1
            val spread = dice.nextDouble()
            particles += SparkParticle(
                x = block.centreX + side * halfSpan * (0.25 + spread * 0.9),
                y = baseY - 0.1 - dice.nextDouble() * 0.35,
                vx = side * (1.4 + dice.nextDouble() * 3.4),
                vy = -0.3 - dice.nextDouble() * 1.1,
                life = 0.55 + dice.nextDouble() * 0.5,
                span = 0.32 + dice.nextDouble() * 0.5,
                tint = SMOKE,
                gravity = 2.6,
                smoky = true,
            )
        }
    }

    /** A ring of gold sparks off the block's upper edge. */
    fun sparkOver(block: SeatedBlock) {
        val count = 16
        repeat(count) { i ->
            val angle = (i.toDouble() / count) * PI * 2
            particles += SparkParticle(
                x = block.centreX,
                y = block.crownY,
                vx = cos(angle) * (2.2 + dice.nextDouble()),
                vy = sin(angle) * (2.2 + dice.nextDouble()),
                life = 0.4 + dice.nextDouble() * 0.3,
                span = 0.1 + dice.nextDouble() * 0.12,
                tint = BrandPalette.Bullion,
                gravity = 1.5,
            )
        }
    }

    /** Coins fountaining out of the tower crown on a successful cash-out. */
    fun coinFountain(crownY: Double) {
        repeat(24) {
            val angle = -PI / 2 + (dice.nextDouble() - 0.5) * 1.6
            val speed = 3.0 + dice.nextDouble() * 4.0
            particles += SparkParticle(
                x = (dice.nextDouble() - 0.5) * 2,
                y = crownY,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.7 + dice.nextDouble() * 0.5,
                span = 0.16 + dice.nextDouble() * 0.12,
                tint = BrandPalette.Bullion,
                gravity = 9.0,
            )
        }
    }

    fun clear() = particles.clear()

    private companion object {
        val SMOKE = Color(0xFFF3EEE3)
    }
}
