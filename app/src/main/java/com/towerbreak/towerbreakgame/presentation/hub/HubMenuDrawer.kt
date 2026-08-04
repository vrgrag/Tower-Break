package com.towerbreak.towerbreakgame.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.towerbreak.towerbreakgame.BuildConfig
import com.towerbreak.towerbreakgame.foundation.text.grouped
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * Slide-in drawer opened by the top-left gear icon. Hosts player summary,
 * navigation shortcuts (Shop/Missions/Ranks), audio toggles, and legal links.
 */
@Composable
fun HubMenuDrawer(
    visible: Boolean,
    rank: Int,
    balance: Int,
    claimableMissions: Int,
    cuesOn: Boolean,
    trackOn: Boolean,
    buzzOn: Boolean,
    onDismiss: () -> Unit,
    onShop: () -> Unit,
    onMissions: () -> Unit,
    onRanks: () -> Unit,
    onToggleCues: () -> Unit,
    onToggleTrack: () -> Unit,
    onToggleBuzz: () -> Unit,
    onPrivacy: () -> Unit,
    onSupport: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it },
            exit = slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(272.dp)
                    .background(Brush.verticalGradient(listOf(BrandPalette.Ribbon, BrandPalette.RibbonDeep)))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 22.dp),
            ) {
                // Player summary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BrandPalette.Bullion),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Lv$rank", style = GameType.tally(size = 12, tint = BrandPalette.RibbonDeep))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Builder", style = GameType.label(size = 16, tint = BrandPalette.White))
                        Text("${balance.grouped()} FUN", style = GameType.prose(size = 12, tint = BrandPalette.Bullion))
                    }
                }

                Spacer(Modifier.height(22.dp))
                DrawerDivider()
                Spacer(Modifier.height(8.dp))

                // Navigation
                DrawerNavRow(Icons.Rounded.ShoppingBag, "Shop") { onDismiss(); onShop() }
                DrawerNavRow(Icons.Rounded.Assignment, "Missions", badge = claimableMissions) { onDismiss(); onMissions() }
                DrawerNavRow(Icons.Rounded.EmojiEvents, "Ranks") { onDismiss(); onRanks() }

                Spacer(Modifier.height(8.dp))
                DrawerDivider()
                Spacer(Modifier.height(8.dp))

                // Audio toggles
                Text("Audio", style = GameType.label(size = 13, tint = BrandPalette.Bullion))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    AudioToggleChip(Icons.Rounded.VolumeUp, "SFX", cuesOn, onToggleCues)
                    AudioToggleChip(Icons.Rounded.MusicNote, "Music", trackOn, onToggleTrack)
                    AudioToggleChip(Icons.Rounded.Vibration, "Buzz", buzzOn, onToggleBuzz)
                }

                Spacer(Modifier.weight(1f))

                DrawerDivider()
                Spacer(Modifier.height(10.dp))
                DrawerNavRow(Icons.Rounded.PrivacyTip, "Privacy", compact = true) { onDismiss(); onPrivacy() }
                DrawerNavRow(Icons.Rounded.SupportAgent, "Support", compact = true) { onDismiss(); onSupport() }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Tower Break  •  v${BuildConfig.VERSION_NAME}",
                    style = GameType.prose(size = 10, tint = BrandPalette.ParchmentFaded.copy(alpha = 0.6f)),
                )
            }
        }
    }
}

@Composable
private fun DrawerDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BrandPalette.Hairline))
}

@Composable
private fun AudioToggleChip(icon: ImageVector, label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) BrandPalette.Bullion.copy(alpha = 0.22f) else BrandPalette.White.copy(alpha = 0.07f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (on) BrandPalette.Bullion else BrandPalette.ParchmentFaded,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = GameType.prose(size = 11, tint = if (on) BrandPalette.White else BrandPalette.ParchmentFaded),
        )
    }
}

@Composable
private fun DrawerNavRow(
    icon: ImageVector,
    label: String,
    badge: Int = 0,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 10.dp else 13.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (compact) BrandPalette.ParchmentFaded else BrandPalette.AzurePale,
            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = if (compact) GameType.prose(size = 13, tint = BrandPalette.ParchmentFaded)
            else GameType.label(size = 15, tint = BrandPalette.White),
            modifier = Modifier.weight(1f),
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPalette.Alarm)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(badge.toString(), style = GameType.tally(size = 11, tint = BrandPalette.White))
            }
            Spacer(Modifier.width(6.dp))
        }
        if (!compact) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = BrandPalette.ParchmentFaded.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
