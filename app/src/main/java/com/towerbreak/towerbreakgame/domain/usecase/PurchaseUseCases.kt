package com.towerbreak.towerbreakgame.domain.usecase

import com.towerbreak.towerbreakgame.domain.model.BackdropTheme
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import javax.inject.Inject

/** Buys a block skin. Already-owned skins succeed without charging. */
class PurchaseSkinUseCase @Inject constructor(
    private val cosmetics: CosmeticsRepository,
    private val wallet: WalletRepository,
) {
    suspend operator fun invoke(skin: Int, price: Int): Boolean {
        if (cosmetics.ownsSkin(skin)) return true
        if (!wallet.debit(price)) return false
        cosmetics.grantSkin(skin)
        return true
    }
}

/** Buys a backdrop. Fails on an unmet rank requirement or a short balance. */
class PurchaseBackdropUseCase @Inject constructor(
    private val cosmetics: CosmeticsRepository,
    private val wallet: WalletRepository,
    private val progression: ProgressionRepository,
) {
    suspend operator fun invoke(theme: BackdropTheme): Boolean {
        if (cosmetics.ownsBackdrop(theme.slot)) return true
        if (progression.state.value.rank < theme.unlockLevel) return false
        if (!wallet.debit(theme.price)) return false
        cosmetics.grantBackdrop(theme.slot)
        return true
    }
}
