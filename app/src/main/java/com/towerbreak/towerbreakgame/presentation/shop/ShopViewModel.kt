package com.towerbreak.towerbreakgame.presentation.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.towerbreak.towerbreakgame.domain.audio.GameAudio
import com.towerbreak.towerbreakgame.domain.audio.SoundEffect
import com.towerbreak.towerbreakgame.domain.model.BackdropTheme
import com.towerbreak.towerbreakgame.domain.model.BlockSkinOffer
import com.towerbreak.towerbreakgame.domain.model.CosmeticsState
import com.towerbreak.towerbreakgame.domain.repository.CosmeticsRepository
import com.towerbreak.towerbreakgame.domain.repository.ProgressionRepository
import com.towerbreak.towerbreakgame.domain.repository.WalletRepository
import com.towerbreak.towerbreakgame.domain.usecase.PurchaseBackdropUseCase
import com.towerbreak.towerbreakgame.domain.usecase.PurchaseSkinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the shop (block skins + backdrops). Every mutation goes through the
 * same [PurchaseSkinUseCase]/[PurchaseBackdropUseCase] the original economy
 * design called for — the screen itself never touches the wallet directly.
 */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val cosmetics: CosmeticsRepository,
    wallet: WalletRepository,
    progression: ProgressionRepository,
    private val purchaseSkin: PurchaseSkinUseCase,
    private val purchaseBackdrop: PurchaseBackdropUseCase,
    private val audio: GameAudio,
) : ViewModel() {

    val cosmeticsState: StateFlow<CosmeticsState> = cosmetics.state
    val balance: StateFlow<Int> = wallet.balance
    val rank: StateFlow<Int> = progression.state
        .map { it.rank }
        .stateIn(viewModelScope, SharingStarted.Eagerly, progression.state.value.rank)

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun onSkinTap(offer: BlockSkinOffer) {
        val state = cosmeticsState.value
        if (offer.skin == state.pinnedSkin) return
        audio.playCue(SoundEffect.TAP)
        if (state.skinsOwned.contains(offer.skin)) {
            viewModelScope.launch { cosmetics.pinSkin(offer.skin) }
            return
        }
        viewModelScope.launch {
            if (purchaseSkin(offer.skin, offer.price)) {
                audio.playCue(SoundEffect.COIN)
            } else {
                flash("Not enough FUN")
            }
        }
    }

    fun onBackdropTap(theme: BackdropTheme) {
        val state = cosmeticsState.value
        if (theme.slot == state.pinnedBackdrop) return
        audio.playCue(SoundEffect.TAP)
        if (state.backdropsOwned.contains(theme.slot)) {
            viewModelScope.launch { cosmetics.pinBackdrop(theme.slot) }
            return
        }
        if (rank.value < theme.unlockLevel) {
            flash("Unlocks at level ${theme.unlockLevel}")
            return
        }
        viewModelScope.launch {
            if (purchaseBackdrop(theme)) {
                audio.playCue(SoundEffect.COIN)
            } else {
                flash("Not enough FUN")
            }
        }
    }

    private fun flash(text: String) { _toast.value = text }
    fun dismissToast() { _toast.value = null }
}
