package com.towerbreak.towerbreakgame.presentation.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.towerbreak.towerbreakgame.foundation.asset.ArtManifest
import com.towerbreak.towerbreakgame.foundation.text.grouped
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.BackdropTheme
import com.towerbreak.towerbreakgame.domain.model.BlockSkinOffer
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * The cosmetics shop: block skins and backdrops, bought or equipped straight
 * from [ShopViewModel] (which itself only calls the purchase use cases).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)

    val cosmetics by viewModel.cosmeticsState.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val rank by viewModel.rank.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); viewModel.dismissToast() }
    }

    Scaffold(
        containerColor = BrandPalette.RibbonDeep,
        topBar = {
            TopAppBar(
                title = { Text("SHOP", style = GameType.label(size = 18)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BrandPalette.Parchment)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandPalette.White.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("${balance.grouped()} FUN", style = GameType.tally(size = 14, tint = BrandPalette.Bullion))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPalette.Ribbon),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionHeader("Block Skins")
            Spacer(Modifier.height(10.dp))
            GridRows(BlockSkinOffer.catalogue) { offer ->
                SkinCard(
                    offer = offer,
                    owned = cosmetics.skinsOwned.contains(offer.skin),
                    pinned = cosmetics.pinnedSkin == offer.skin,
                    affordable = balance >= offer.price,
                    onTap = { viewModel.onSkinTap(offer) },
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Backdrops")
            Spacer(Modifier.height(10.dp))
            GridRows(BackdropTheme.entries.toList()) { theme ->
                BackdropCard(
                    theme = theme,
                    owned = cosmetics.backdropsOwned.contains(theme.slot),
                    pinned = cosmetics.pinnedBackdrop == theme.slot,
                    unlocked = rank >= theme.unlockLevel,
                    affordable = balance >= theme.price,
                    onTap = { viewModel.onBackdropTap(theme) },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text.uppercase(), style = GameType.label(size = 15, tint = BrandPalette.Bullion))
}

/** Lays [items] out two-per-row — small, fixed catalogues don't need a lazy grid. */
@Composable
private fun <T> GridRows(items: List<T>, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { item ->
                    Box(Modifier.weight(1f)) { cell(item) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SkinCard(offer: BlockSkinOffer, owned: Boolean, pinned: Boolean, affordable: Boolean, onTap: () -> Unit) {
    ShopCard(pinned = pinned, onTap = onTap) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "file:///android_asset/${ArtManifest.block(offer.skin)}",
                contentDescription = offer.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        CardFooter(
            title = offer.displayName,
            owned = owned,
            pinned = pinned,
            price = offer.price,
            affordable = affordable,
            lockedLabel = null,
        )
    }
}

@Composable
private fun BackdropCard(
    theme: BackdropTheme,
    owned: Boolean,
    pinned: Boolean,
    unlocked: Boolean,
    affordable: Boolean,
    onTap: () -> Unit,
) {
    ShopCard(pinned = pinned, onTap = onTap) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .background(Brush.verticalGradient(listOf(theme.high, theme.low))),
        )
        CardFooter(
            title = theme.displayName,
            owned = owned,
            pinned = pinned,
            price = theme.price,
            affordable = affordable,
            lockedLabel = if (!unlocked && !owned) "Lv ${theme.unlockLevel}" else null,
        )
    }
}

@Composable
private fun ShopCard(pinned: Boolean, onTap: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BrandPalette.Panel)
            .border(
                width = if (pinned) 2.dp else 1.dp,
                color = if (pinned) BrandPalette.Bullion else BrandPalette.Hairline,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onTap),
    ) { content() }
}

@Composable
private fun CardFooter(title: String, owned: Boolean, pinned: Boolean, price: Int, affordable: Boolean, lockedLabel: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(title, style = GameType.label(size = 13, tint = BrandPalette.White))
        Spacer(Modifier.height(4.dp))
        when {
            pinned -> StatusChip("EQUIPPED", BrandPalette.Moss, Icons.Rounded.CheckCircle)
            owned -> StatusChip("EQUIP", BrandPalette.Azure, null)
            lockedLabel != null -> StatusChip(lockedLabel, BrandPalette.ParchmentFaded, Icons.Rounded.Lock)
            price == 0 -> StatusChip("FREE", BrandPalette.Moss, null)
            else -> StatusChip(
                text = "${price.grouped()} FUN",
                tint = if (affordable) BrandPalette.Bullion else BrandPalette.Alarm,
                icon = null,
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, tint: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.height(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = GameType.tally(size = 11, tint = tint))
    }
}
