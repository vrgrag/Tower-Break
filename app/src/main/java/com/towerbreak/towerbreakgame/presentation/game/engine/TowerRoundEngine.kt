package com.towerbreak.towerbreakgame.presentation.game.engine

import com.towerbreak.towerbreakgame.domain.model.HazardBand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.random.Random

/**
 * The round loop (the Flutter `SiegeSim`).
 *
 * A gantry sweeps a block across the playfield; a BUILD tap releases it. Whether
 * the storey holds is pure chance, weighted by the [HazardBand] — there is no aim
 * or timing window. A storey that holds rolls its own multiplier and compounds
 * the payout; one that fails ends the round.
 *
 * Fingerprint / architecture changes vs. the Flutter original:
 *  - Not a `ChangeNotifier`. It publishes two immutable `StateFlow`s (a per-frame
 *    [snapshot] for the renderer and a discrete [hud] for the overlays) plus a
 *    one-shot [events] `SharedFlow`. This is the unidirectional-data-flow model
 *    the rest of the app already speaks.
 *  - Side effects (audio, meta-game) are *not* fired from here. The engine only
 *    emits [RoundEvent]s; the ViewModel turns those into cues and use-case calls.
 *  - The "reveal result after a beat" delay is frame-integrated off [elapsed]
 *    rather than a scheduled future, keeping the engine deterministic and
 *    driver-agnostic (a test can just pump `step(dt)`).
 */
class TowerRoundEngine(
    private val metrics: BlockMetrics,
    initialBand: HazardBand,
    private val nextSkin: () -> Int,
    private val dice: Random = Random.Default,
) {
    private val gantry = GantrySweep()
    private val lens = ViewportTracker(PlayfieldConfig.pedestalCrown)
    private val fx = ParticleFactory(dice)

    private val _snapshot = MutableStateFlow(PlayfieldSnapshot())
    val snapshot: StateFlow<PlayfieldSnapshot> = _snapshot.asStateFlow()

    private val _hud = MutableStateFlow(RoundHudState())
    val hud: StateFlow<RoundHudState> = _hud.asStateFlow()

    private val _events = MutableSharedFlow<RoundEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<RoundEvent> = _events.asSharedFlow()

    var band: HazardBand = initialBand
        private set

    private var stage = RoundStage.BETTING
    private var stake = 0
    private var storey = 0
    private var payoutMultiplier = 0.0
    private var storeyMultiplier = 0.0
    private var resultReady = false

    private val rolls = ArrayDeque<Double>()
    private val stack = mutableListOf<SeatedBlock>()
    private var falling: FallingBlock? = null

    private var carriedSkin = 1
    private var cloudDrift = 0.0
    private var tremor = 0.0

    private var elapsed = 0.0
    private var resultArmedAt: Double? = null
    private var resultArmStage: RoundStage? = null

    init {
        carriedSkin = nextSkin()
        publishAll()
    }

    // --- derived read-only view ---------------------------------------------
    private val crownY: Double
        get() = if (stack.isEmpty()) PlayfieldConfig.pedestalCrown else stack.last().crownY

    private val carriesBlock: Boolean get() = stage.carriesBlock
    private val carriedRise: Double get() = metrics.blockRise(carriedSkin)
    private val craneX: Double get() = if (carriesBlock) gantry.sweepOffset else gantry.parkedX
    private val hangCentreY: Double
        get() = crownY - PlayfieldConfig.HANG_CLEARANCE - carriedRise / 2
    private val potential: Int get() = floor(stake * payoutMultiplier).toInt()
    private val swingPeriod: Double get() = band.swingPeriodAt(storey)

    // --- controls -----------------------------------------------------------

    /** Swaps the hazard band mid-session; takes effect from the next swing. */
    fun retune(band: HazardBand) {
        this.band = band
        publishHud()
    }

    /** Starts a round for an already-debited [stake], releasing the first block. */
    fun beginWager(stake: Int) {
        if (stage != RoundStage.BETTING) return
        this.stake = stake
        wipeBoard()
        stage = RoundStage.SWINGING
        release()
    }

    /** The BUILD tap. Only meaningful while a block is being carried. */
    fun tapBuild() {
        if (stage != RoundStage.SWINGING) return
        release()
    }

    /** Banks the winnings. Requires at least one standing storey. */
    fun cashOut() {
        if (stage != RoundStage.SWINGING || storey < 1) return
        val payout = potential
        stage = RoundStage.BANKED
        resultReady = false
        tremor = 0.3
        fx.coinFountain(crownY)
        _events.tryEmit(RoundEvent.Banked(storey, payout))
        armResult()
        publishAll()
    }

    /** Clears the tower and returns to the betting state. */
    fun resetRound() {
        stage = RoundStage.BETTING
        wipeBoard()
        carriedSkin = nextSkin()
        publishAll()
    }

    private fun wipeBoard() {
        resultReady = false
        resultArmedAt = null
        resultArmStage = null
        storey = 0
        payoutMultiplier = 0.0
        storeyMultiplier = 0.0
        rolls.clear()
        stack.clear()
        fx.clear()
        falling = null
        lens.snapTo(PlayfieldConfig.pedestalCrown)
    }

    private fun release() {
        val launchX = craneX
        val skin = carriedSkin
        val rise = metrics.blockRise(skin)
        gantry.park(launchX)

        // Pure chance: the band's hold probability alone decides the storey.
        val holds = dice.nextDouble() <= band.holdChance
        val s = sign(launchX)
        val lean = if (s == 0.0) 1.0 else s

        falling = FallingBlock(
            skin = skin,
            x = launchX,
            y = hangCentreY,
            vy = PlayfieldConfig.FALL_LAUNCH_SPEED,
            span = PlayfieldConfig.BLOCK_SPAN,
            rise = rise,
            seatY = crownY - rise / 2,
            doomed = !holds,
            vx = if (holds) 0.0 else lean * 1.4,
            spin = if (holds) 0.0 else lean * 1.8,
        )
        stage = RoundStage.PLUNGING
        _events.tryEmit(RoundEvent.BlockReleased)
        publishAll()
    }

    // --- frame ---------------------------------------------------------------

    /** One simulation step. [dt] is clamped so a stalled frame cannot teleport. */
    fun step(dt: Double) {
        val clamped = dt.coerceIn(0.0, PlayfieldConfig.MAX_STEP)
        elapsed += clamped

        cloudDrift += clamped * 0.04
        if (tremor > 0) tremor = max(0.0, tremor - clamped * 1.2)

        if (carriesBlock) gantry.advance(clamped, swingPeriod)

        val f = falling
        if (stage == RoundStage.PLUNGING && f != null) advancePlunge(f, clamped)

        lens.ease(clamped, crownY)
        fx.advance(clamped)
        maybeRevealResult()

        publishSnapshot()
    }

    private fun advancePlunge(block: FallingBlock, dt: Double) {
        block.vy += PlayfieldConfig.FALL_ACCEL * dt
        block.y += block.vy * dt
        block.x += block.vx * dt
        block.angle += block.spin * dt

        if (!block.doomed) {
            // Ease back toward centre so a surviving block always seats aligned.
            block.x += (0 - block.x) * min(1.0, dt * 9)
            if (block.y >= block.seatY) seat(block)
            return
        }

        if (stage == RoundStage.PLUNGING && block.y > block.seatY + block.rise * 0.9) {
            collapse()
        }
        if (block.y > lens.y + PlayfieldConfig.TUMBLE_DEPTH) falling = null
    }

    private fun seat(block: FallingBlock) {
        val seatCrown = crownY - block.rise
        val seated = SeatedBlock(
            skin = block.skin,
            centreX = 0.0,
            crownY = seatCrown,
            span = block.span,
            rise = block.rise,
        )
        stack += seated
        falling = null
        storey += 1

        // Each storey rolls its own multiplier; the payout is the running product.
        storeyMultiplier = band.rollMultiplier(dice)
        payoutMultiplier = if (payoutMultiplier <= 0) storeyMultiplier else payoutMultiplier * storeyMultiplier
        payoutMultiplier = Math.round(payoutMultiplier * 100).toDouble() / 100.0
        rolls.addFirst(storeyMultiplier)
        tremor = 0.3

        fx.puffUnder(seated)
        fx.sparkOver(seated)

        _events.tryEmit(RoundEvent.StoreySeated(storey))

        carriedSkin = nextSkin()
        stage = RoundStage.SWINGING
        publishAll()
    }

    private fun collapse() {
        stage = RoundStage.COLLAPSED
        resultReady = false
        tremor = 0.6
        _events.tryEmit(RoundEvent.Collapsed(storey))
        armResult()
        publishAll()
    }

    /** Holds the result panel back for a beat so the headline reads first. */
    private fun armResult() {
        resultArmedAt = elapsed
        resultArmStage = stage
    }

    private fun maybeRevealResult() {
        val armedAt = resultArmedAt ?: return
        if (resultReady) return
        if (stage != resultArmStage) {
            resultArmedAt = null
            return
        }
        if (elapsed - armedAt >= 0.85) {
            resultReady = true
            resultArmedAt = null
            publishHud()
        }
    }

    // --- publishing ----------------------------------------------------------

    private fun publishAll() {
        publishHud()
        publishSnapshot()
    }

    private fun publishSnapshot() {
        _snapshot.value = PlayfieldSnapshot(
            stage = stage,
            storey = storey,
            lensY = lens.y,
            cloudDrift = cloudDrift,
            tremor = tremor,
            craneX = craneX,
            carriesBlock = carriesBlock,
            carriedSkin = carriedSkin,
            carriedRise = carriedRise,
            hangCentreY = hangCentreY,
            seated = stack.toList(),
            falling = falling,
            particles = fx.particles.toList(),
        )
    }

    private fun publishHud() {
        _hud.value = RoundHudState(
            stage = stage,
            storey = storey,
            payoutMultiplier = payoutMultiplier,
            storeyMultiplier = storeyMultiplier,
            potential = potential,
            stake = stake,
            canBank = stage == RoundStage.SWINGING && storey >= 1,
            rolls = rolls.toList(),
            resultReady = resultReady,
        )
    }
}
