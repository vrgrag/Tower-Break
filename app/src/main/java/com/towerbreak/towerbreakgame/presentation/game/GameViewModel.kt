package com.towerbreak.towerbreakgame.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.MusicBed
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.model.BackdropTheme
import com.towerbreak.towerbreakgame.domain.model.GameTuning
import com.towerbreak.towerbreakgame.domain.model.HazardBand
import com.towerbreak.towerbreakgame.domain.model.PromotionResult
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.SettingsRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import com.towerbreak.towerbreakgame.domain.usecase.SettleCashOutUseCase
import com.towerbreak.towerbreakgame.domain.usecase.SettleCollapseUseCase
import com.towerbreak.towerbreakgame.domain.usecase.SettleSeatedStoreyUseCase
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundEvent
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundHudState
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundStage
import com.towerbreak.towerbreakgame.presentation.game.engine.PlayfieldSnapshot
import com.towerbreak.towerbreakgame.presentation.game.engine.TowerRoundEngine
import com.towerbreak.towerbreakgame.presentation.game.render.PlayfieldTextures
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wires the playable round together (the Flutter `_ArenaStageState`).
 *
 * Fingerprint / architecture changes:
 *  - The `Ticker` becomes a `withFrameNanos` loop the screen drives via
 *    [advance]; the sim's frame integration is otherwise unchanged.
 *  - The screen no longer inlines meta-game side effects. The engine emits typed
 *    [RoundEvent]s which this ViewModel maps to audio cues and single-purpose use
 *    cases — the arena widget used to call five stores directly.
 *  - State reaches the UI as `StateFlow`s (playfield, hud, chrome) instead of a
 *    grab-bag of `ValueListenable`s and `AnimatedBuilder`s.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    // Exposed so the canvas can read decoded bitmaps directly; the engine only
    // ever sees it through the [BlockMetrics] interface.
    val textures: PlayfieldTextures,
    private val audio: GameAudio,
    private val wallet: WalletRepository,
    progression: ProgressionRepository,
    private val cosmetics: CosmeticsRepository,
    private val settings: SettingsRepository,
    private val missions: DailyMissionRepository,
    private val settleSeatedStorey: SettleSeatedStoreyUseCase,
    private val settleCollapse: SettleCollapseUseCase,
    private val settleCashOut: SettleCashOutUseCase,
) : ViewModel() {

    private var rotationCursor = 0

    private val engine = TowerRoundEngine(
        metrics = textures,
        initialBand = settings.state.value.band,
        nextSkin = ::nextSkin,
    )

    val playfield: StateFlow<PlayfieldSnapshot> = engine.snapshot
    val hud: StateFlow<RoundHudState> = engine.hud

    val balance: StateFlow<Int> = wallet.balance
    val rank: StateFlow<Int> = progression.state
        .map { it.rank }
        .stateIn(viewModelScope, SharingStarted.Eagerly, progression.state.value.rank)
    val backdrop: StateFlow<BackdropTheme> = cosmetics.state
        .map { it.backdrop }
        .stateIn(viewModelScope, SharingStarted.Eagerly, cosmetics.state.value.backdrop)
    val band: StateFlow<HazardBand> = settings.state
        .map { it.band }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.state.value.band)

    private val _stake = MutableStateFlow(initialStake())
    val stake: StateFlow<Int> = _stake.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _ready = MutableStateFlow(textures.ready)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var toastJob: Job? = null

    init {
        viewModelScope.launch {
            missions.rollOverIfStale()
            textures.warmUp()
            _ready.value = true
        }
        audio.playBed(MusicBed.ARENA)
        collectEvents()
    }

    /**
     * Advances the simulation by one frame. Driven from the screen's
     * `LaunchedEffect` + `withFrameNanos` loop, because the Compose frame clock
     * only exists inside a composition-scoped coroutine — not in [viewModelScope].
     */
    fun advance(dt: Double) = engine.step(dt)

    private fun collectEvents() {
        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    RoundEvent.BlockReleased -> audio.playCue(SoundEffect.RELEASE)
                    is RoundEvent.StoreySeated -> onStoreySeated(event.storey)
                    is RoundEvent.Collapsed -> onCollapse(event.storey)
                    is RoundEvent.Banked -> onBanked(event.storey, event.payout)
                }
            }
        }
    }

    // --- event side effects --------------------------------------------------
    private fun onStoreySeated(storey: Int) {
        audio.playCue(SoundEffect.SEAT)
        audio.buzz()
        viewModelScope.launch {
            delay(60)
            audio.playCue(SoundEffect.CHIME)
        }
        viewModelScope.launch {
            val report = settleSeatedStorey(storey)
            if (report.promoted) celebrate(report)
        }
    }

    private fun onCollapse(storey: Int) {
        audio.playCue(SoundEffect.THUD)
        audio.buzz(heavy = true)
        viewModelScope.launch { settleCollapse(storey) }
    }

    private fun onBanked(storey: Int, payout: Int) {
        audio.playCue(SoundEffect.TRIUMPH)
        audio.buzz()
        viewModelScope.launch {
            val report = settleCashOut(storey, payout)
            if (report.promoted) celebrate(report)
        }
    }

    private fun celebrate(report: PromotionResult) {
        audio.playCue(SoundEffect.FANFARE)
        val extra = if (report.freshSkins.isEmpty()) "" else "  •  New skin unlocked!"
        flash("LEVEL ${report.rank}!  +${report.bounty} FUN$extra")
    }

    private fun flash(text: String) {
        toastJob?.cancel()
        _toast.value = text
        toastJob = viewModelScope.launch {
            delay(3000)
            _toast.value = null
        }
    }

    // --- player actions ------------------------------------------------------
    fun onBuild() {
        when (hud.value.stage) {
            RoundStage.BETTING -> {
                val amount = _stake.value
                if (!wallet.canAfford(amount)) {
                    flash("Not enough FUN — claim your daily bonus!")
                    audio.playCue(SoundEffect.THUD)
                    return
                }
                audio.playCue(SoundEffect.TAP)
                viewModelScope.launch {
                    wallet.debit(amount)
                    settings.rememberStake(amount)
                    engine.beginWager(amount)
                }
            }
            RoundStage.SWINGING -> engine.tapBuild()
            RoundStage.PLUNGING, RoundStage.BANKED, RoundStage.COLLAPSED -> Unit
        }
    }

    fun onBank() = engine.cashOut()

    fun onReplay() {
        audio.playCue(SoundEffect.TAP)
        engine.resetRound()
    }

    fun onChooseBand(band: HazardBand) {
        audio.playCue(SoundEffect.TAP)
        viewModelScope.launch { settings.chooseBand(band) }
        engine.retune(band)
    }

    fun onStakeChange(value: Int) { _stake.value = value }

    fun onNudge() = audio.playCue(SoundEffect.TAP)

    fun onLeave() = audio.playCue(SoundEffect.TAP)

    // --- helpers -------------------------------------------------------------
    private fun initialStake(): Int {
        val balance = wallet.balance.value
        var stake = settings.state.value.stake.coerceIn(GameTuning.STAKE_FLOOR, GameTuning.STAKE_CEIL)
        if (balance >= GameTuning.STAKE_FLOOR && stake > balance) stake = balance
        return stake
    }

    /** Honours a pinned skin, otherwise cycles through everything owned. */
    private fun nextSkin(): Int {
        val owned = cosmetics.state.value.skinsOwned
        if (owned.isEmpty()) return 1
        val pinned = cosmetics.state.value.pinnedSkin
        if (pinned != 0 && owned.contains(pinned)) return pinned
        val skin = owned[rotationCursor % owned.size]
        rotationCursor++
        return skin
    }
}
