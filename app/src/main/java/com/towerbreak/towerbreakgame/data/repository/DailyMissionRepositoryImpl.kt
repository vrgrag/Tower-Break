package com.towerbreak.towerbreakgame.data.repository

import com.towerbreak.towerbreakgame.foundation.time.GameClock
import com.towerbreak.towerbreakgame.data.local.PreferenceStore
import com.towerbreak.towerbreakgame.domain.model.DailyMission
import com.towerbreak.towerbreakgame.domain.model.MissionKind
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Builds and tracks a board of three missions per calendar day. The set is drawn
 * deterministically from the date, so it survives restarts without storing the
 * draw itself, and it rolls over at local midnight.
 *
 * Fingerprint note: persistence uses kotlinx.serialization DTOs (with the same
 * short JSON keys as the Dart `encode()`), and the board is an immutable list
 * republished on every change instead of a list of mutable `Quest` objects.
 */
@Singleton
class DailyMissionRepositoryImpl @Inject constructor(
    private val prefs: PreferenceStore,
    private val wallet: WalletRepository,
    private val audio: GameAudio,
    private val clock: GameClock,
) : DailyMissionRepository, Hydratable {

    @Serializable
    private data class MissionDto(
        @SerialName("t") val kind: Int,
        @SerialName("g") val target: Int,
        @SerialName("r") val reward: Int,
        @SerialName("p") val tally: Int,
        @SerialName("c") val claimed: Boolean,
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val _board = MutableStateFlow<List<DailyMission>>(emptyList())
    override val board: StateFlow<List<DailyMission>> = _board.asStateFlow()

    private val _claimable = MutableStateFlow(0)
    override val claimableCount: StateFlow<Int> = _claimable.asStateFlow()

    private var cashOutRun = 0

    override suspend fun hydrate() {
        val today = clock.todayStamp()
        val blob = prefs.readString(PreferenceStore.Keys.missionBlob)
        if (prefs.readString(PreferenceStore.Keys.missionStamp) == today && !blob.isNullOrEmpty()) {
            runCatching {
                _board.value = json.decodeFromString<List<MissionDto>>(blob).map(::fromDto)
                publishClaimable()
                return
            }
        }
        _board.value = draw(today)
        save(today)
    }

    override suspend fun rollOverIfStale() {
        if (prefs.readString(PreferenceStore.Keys.missionStamp) == clock.todayStamp()) return
        cashOutRun = 0
        _board.value = draw(clock.todayStamp())
        save()
    }

    override suspend fun onStoreySeated() = bump { mission ->
        if (mission.kind.tracksSeating) min(mission.target, mission.tally + 1) else null
    }

    override suspend fun onStoreyReached(storey: Int) = bump { mission ->
        if (mission.kind == MissionKind.REACH_STOREY) {
            max(mission.tally, min(mission.target, storey))
        } else null
    }

    override suspend fun onCashOut() {
        cashOutRun += 1
        bump { mission ->
            if (mission.kind == MissionKind.CASH_OUT_RUN) min(mission.target, cashOutRun) else null
        }
    }

    override suspend fun onRoundLost() {
        cashOutRun = 0
        bump { mission -> if (mission.kind == MissionKind.CASH_OUT_RUN) 0 else null }
    }

    override suspend fun collect(mission: DailyMission): Int {
        val index = _board.value.indexOfFirst { it == mission }
        if (index < 0) return 0
        val target = _board.value[index]
        if (!target.done || target.claimed) return 0

        _board.value = _board.value.toMutableList().also {
            it[index] = target.copy(claimed = true)
        }
        wallet.credit(target.reward)
        audio.playCue(SoundEffect.COIN)
        save()
        return target.reward
    }

    /**
     * Applies [next] to every mission it returns a value for, skipping ones that
     * are already finished so a completed mission can never regress.
     */
    private suspend fun bump(next: (DailyMission) -> Int?) {
        _board.value = _board.value.map { mission ->
            if (mission.done) return@map mission
            next(mission)?.let { mission.copy(tally = it) } ?: mission
        }
        save()
    }

    private fun draw(stamp: String): List<DailyMission> {
        val dice = Random(stamp.hashCode())
        val pool = listOf(
            DailyMission(MissionKind.STOREYS_SEATED, target = 12, reward = 200),
            DailyMission(MissionKind.STOREYS_CLIMBED, target = 25, reward = 350),
            DailyMission(MissionKind.CASH_OUT_RUN, target = 3, reward = 300),
            DailyMission(MissionKind.REACH_STOREY, target = 8, reward = 250),
            DailyMission(MissionKind.STOREYS_SEATED, target = 20, reward = 320),
            DailyMission(MissionKind.REACH_STOREY, target = 12, reward = 450),
        )
        return pool.shuffled(dice).take(3)
    }

    private suspend fun save(stamp: String? = null) {
        prefs.writeString(PreferenceStore.Keys.missionStamp, stamp ?: clock.todayStamp())
        prefs.writeString(
            PreferenceStore.Keys.missionBlob,
            json.encodeToString(_board.value.map(::toDto)),
        )
        publishClaimable()
    }

    private fun publishClaimable() {
        _claimable.value = _board.value.count { it.done && !it.claimed }
    }

    private fun toDto(m: DailyMission) =
        MissionDto(m.kind.ordinal, m.target, m.reward, m.tally, m.claimed)

    private fun fromDto(dto: MissionDto) = DailyMission(
        kind = MissionKind.entries[dto.kind],
        target = dto.target,
        reward = dto.reward,
        tally = dto.tally,
        claimed = dto.claimed,
    )
}
