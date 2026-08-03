package com.towerbreak.towerbreakgame.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.towerbreak.towerbreakgame.core.text.grouped
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.ExternalPage
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.components.CandyButton
import com.towerbreak.towerbreakgame.presentation.common.components.CandySkin
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * Main menu — redesigned with a 2×2 feature grid (matching reference screenshot):
 * a floating top-bar, the game logo, a live XP-progress strip, a big PLAY button,
 * four coloured shortcut tiles, and a drawer behind the gear icon.
 */
@Composable
fun HubScreen(
    onPlay: () -> Unit,
    onOpenPage: (ExternalPage) -> Unit,
    onOpenShop: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenRanks: () -> Unit,
    viewModel: HubViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)

    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val rank by viewModel.rank.collectAsStateWithLifecycle()
    val progression by viewModel.progressionState.collectAsStateWithLifecycle()
    val collectable by viewModel.collectable.collectAsStateWithLifecycle()
    val nextPayout by viewModel.nextPayout.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val claimableMissions by viewModel.claimableMissions.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var drawerOpen by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); viewModel.dismissToast() }
    }

    Box(Modifier.fillMaxSize().background(BrandPalette.RibbonDeep)) {

        AsyncImage(
            model = "file:///android_asset/gameplay/start_bg_asset.webp",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.36f to Color.Black.copy(alpha = 0.10f),
                    0.64f to Color.Black.copy(alpha = 0.18f),
                    1f to Color.Black.copy(alpha = 0.84f),
                ),
            ),
        )
        FloatingEmbers(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HubTopBar(
                rank = rank,
                balance = balance,
                onSettingsTap = { viewModel.clickCue(); drawerOpen = true },
            )

            Spacer(Modifier.height(4.dp))

            AsyncImage(
                model = "file:///android_asset/ui/Game_Name.webp",
                contentDescription = "Tower Break",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(144.dp),
            )

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                XpProgressStrip(
                    rank = rank,
                    fraction = progression.fractionThroughRank,
                    into = progression.intoRank,
                    span = progression.rankSpan,
                )

                Spacer(Modifier.height(12.dp))

                CandyButton(
                    label = "PLAY",
                    skin = CandySkin.MOSS,
                    height = 68.dp,
                    fontSize = 24,
                    fillWidth = true,
                    icon = Icons.Rounded.PlayArrow,
                    onPressed = { viewModel.clickCue(); onPlay() },
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NotifBadgeBox(
                        badge = if (collectable) 1 else 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        CandyButton(
                            label = "Daily",
                            skin = CandySkin.AMBER,
                            height = 64.dp,
                            fontSize = 18,
                            fillWidth = true,
                            icon = Icons.Rounded.CardGiftcard,
                            footnote = if (collectable) "+$nextPayout FUN" else null,
                            onPressed = { viewModel.claimDailyBonus() },
                        )
                    }
                    NotifBadgeBox(
                        badge = claimableMissions,
                        modifier = Modifier.weight(1f),
                    ) {
                        CandyButton(
                            label = "Missions",
                            skin = CandySkin.AZURE,
                            height = 64.dp,
                            fontSize = 18,
                            fillWidth = true,
                            icon = Icons.Rounded.Assignment,
                            onPressed = { viewModel.clickCue(); onOpenMissions() },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        CandyButton(
                            label = "Shop",
                            skin = CandySkin.MOSS,
                            height = 64.dp,
                            fontSize = 18,
                            fillWidth = true,
                            icon = Icons.Rounded.ShoppingBag,
                            onPressed = { viewModel.clickCue(); onOpenShop() },
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CandyButton(
                            label = "Ranks",
                            skin = CandySkin.VIOLET,
                            height = 64.dp,
                            fontSize = 18,
                            fillWidth = true,
                            icon = Icons.Rounded.EmojiEvents,
                            onPressed = { viewModel.clickCue(); onOpenRanks() },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FooterLink("Privacy Policy") { onOpenPage(ExternalPage.PRIVACY) }
                Text(
                    "•",
                    style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded.copy(alpha = 0.5f)),
                )
                FooterLink("Support") { onOpenPage(ExternalPage.SUPPORT) }
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))

        HubMenuDrawer(
            visible = drawerOpen,
            rank = rank,
            balance = balance,
            claimableMissions = claimableMissions,
            cuesOn = settings.cuesOn,
            trackOn = settings.trackOn,
            buzzOn = settings.buzzOn,
            onDismiss = { drawerOpen = false },
            onShop = onOpenShop,
            onMissions = onOpenMissions,
            onRanks = onOpenRanks,
            onToggleCues = viewModel::toggleCues,
            onToggleTrack = viewModel::toggleTrack,
            onToggleBuzz = viewModel::toggleBuzz,
            onPrivacy = { onOpenPage(ExternalPage.PRIVACY) },
            onSupport = { onOpenPage(ExternalPage.SUPPORT) },
        )
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────────

@Composable
private fun HubTopBar(rank: Int, balance: Int, onSettingsTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(BrandPalette.White.copy(alpha = 0.12f), CircleShape)
                .clickable(onClick = onSettingsTap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = BrandPalette.White, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .background(BrandPalette.Azure, RoundedCornerShape(20.dp))
                .border(1.5.dp, BrandPalette.AzureDeep, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = BrandPalette.Bullion, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("Lv $rank", style = GameType.label(size = 14, tint = BrandPalette.White))
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .background(BrandPalette.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        Brush.linearGradient(listOf(BrandPalette.Bullion, Color(0xFFE0A11A))),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("F", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A5300))
            }
            Text(balance.grouped(), style = GameType.tally(size = 17, tint = BrandPalette.White))
            Text("FUN", style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded))
        }
    }
}

// ── XP progress strip ────────────────────────────────────────────────────────

@Composable
private fun XpProgressStrip(
    rank: Int,
    fraction: Float,
    into: Int,
    span: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.48f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = BrandPalette.Bullion, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text("Lv $rank", style = GameType.label(size = 14, tint = BrandPalette.White))
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BrandPalette.White.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(BrandPalette.Bullion),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "$into/$span",
            style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded),
        )
    }
}

// ── Notification badge wrapper ────────────────────────────────────────────────

/** Overlays a red dot on its [content] when [badge] > 0. */
@Composable
private fun NotifBadgeBox(badge: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        content()
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = (-4).dp)
                    .size(16.dp)
                    .background(BrandPalette.Alarm, CircleShape),
            )
        }
    }
}

// ── Footer link ───────────────────────────────────────────────────────────────

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        style = GameType.prose(size = 12, tint = BrandPalette.ParchmentFaded),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
