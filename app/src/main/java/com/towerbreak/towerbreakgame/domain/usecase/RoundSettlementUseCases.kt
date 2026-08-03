package com.towerbreak.towerbreakgame.domain.usecase

import com.towerbreak.towerbreakgame.domain.model.GameTuning
import com.towerbreak.towerbreakgame.domain.model.PromotionResult
import com.towerbreak.towerbreakgame.domain.repository.DailyMissionRepository
import com.towerbreak.towerbreakgame.domain.repository.LeaderboardRepository
import com.towerbreak.towerbreakgame.domain.repository.MilestoneRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * The three round-lifecycle side effects the arena used to inline. Each is its
 * own use case so the ViewModel just forwards engine callbacks and never touches
 * a repository directly.
 */

/** A storey seated: tick the seating/height missions and award storey XP. */
class SettleSeatedStoreyUseCase @Inject constructor(
    private val missions: DailyMissionRepository,
    private val awardExperience: AwardExperienceUseCase,
) {
    suspend operator fun invoke(storey: Int): PromotionResult {
        missions.onStoreySeated()
        missions.onStoreyReached(storey)
        return awardExperience(GameTuning.XP_PER_STOREY)
    }
}

/** The tower collapsed: break the cash-out run and log a zero-payout round. */
class SettleCollapseUseCase @Inject constructor(
    private val missions: DailyMissionRepository,
    private val milestones: MilestoneRepository,
    private val leaderboard: LeaderboardRepository,
) {
    suspend operator fun invoke(storey: Int) {
        missions.onRoundLost()
        milestones.logRound(payout = 0, height = storey, streak = storey)
        leaderboard.logRound(height = storey, payout = 0, streak = storey)
    }
}

/** The player banked: pay out, tick the cash-out run, award XP and log records. */
class SettleCashOutUseCase @Inject constructor(
    private val missions: DailyMissionRepository,
    private val wallet: WalletRepository,
    private val awardExperience: AwardExperienceUseCase,
    private val milestones: MilestoneRepository,
    private val leaderboard: LeaderboardRepository,
) {
    suspend operator fun invoke(storey: Int, payout: Int): PromotionResult {
        missions.onCashOut()
        wallet.credit(payout)
        val report = awardExperience(GameTuning.XP_PER_CASH_OUT)
        milestones.logRound(payout = payout, height = storey, streak = storey)
        leaderboard.logRound(height = storey, payout = payout, streak = storey)
        return report
    }
}
