package com.towerbreak.towerbreakgame.domain.usecase

import com.towerbreak.towerbreakgame.domain.model.LevelCurve
import com.towerbreak.towerbreakgame.domain.model.PromotionResult
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Banks experience and settles every promotion it triggers: coins for each rung
 * crossed plus any block skins those rungs gift.
 *
 * Fingerprint note: this is the Flutter `PlayerProfile.grantExperience`, but
 * extracted into a single-responsibility use case that composes three repository
 * interfaces. The domain no longer has a god-object that owns every store.
 */
class AwardExperienceUseCase @Inject constructor(
    private val progression: ProgressionRepository,
    private val cosmetics: CosmeticsRepository,
    private val wallet: WalletRepository,
) {
    suspend operator fun invoke(amount: Int): PromotionResult {
        if (amount <= 0) return PromotionResult.None

        val before = progression.state.value.rank
        progression.bank(amount)
        val after = progression.state.value.rank
        if (after <= before) {
            return PromotionResult(promoted = false, rank = after, bounty = 0, freshSkins = emptyList())
        }

        var bounty = 0
        val fresh = mutableListOf<Int>()
        for (rung in (before + 1)..after) {
            bounty += LevelCurve.bounty(rung)
            for (skin in LevelCurve.giftedSkinsAt(rung)) {
                if (cosmetics.ownsSkin(skin)) continue
                cosmetics.grantSkin(skin)
                fresh += skin
            }
        }
        if (bounty > 0) wallet.credit(bounty)

        return PromotionResult(promoted = true, rank = after, bounty = bounty, freshSkins = fresh)
    }
}
