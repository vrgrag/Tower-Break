package com.towerbreak.towerbreakgame.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.towerbreak.towerbreakgame.core.text.grouped
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * The dark, caution-trimmed bar pinned to the top of every screen (the Flutter
 * `StatusRibbon`): a leading icon button, the player's rank badge and their FUN
 * balance.
 */
@Composable
fun StatusRibbon(
    coins: Int,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    leadingIcon: ImageVector,
    onLeading: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BrandPalette.Ribbon, BrandPalette.RibbonDeep))),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIcon(leadingIcon, onLeading)
            if (rank != null) {
                Spacer(Modifier.width(10.dp))
                RankBadge(rank)
            }
            Spacer(Modifier.weight(1f))
            CoinPill(coins)
        }
        CautionStripe()
    }
}

@Composable
private fun GlassIcon(icon: ImageVector, onTap: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(BrandPalette.White.copy(alpha = 0.08f), CircleShape)
            .clickable(enabled = onTap != null) { onTap?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = BrandPalette.Parchment, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun RankBadge(rank: Int) {
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
}

@Composable
private fun CoinPill(coins: Int) {
    Row(
        modifier = Modifier
            .background(BrandPalette.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Brush.linearGradient(listOf(BrandPalette.Bullion, Color(0xFFE0A11A))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("F", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A5300))
        }
        Text(coins.grouped(), style = GameType.tally(size = 17, tint = BrandPalette.White))
        Text("FUN", style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded))
    }
}
