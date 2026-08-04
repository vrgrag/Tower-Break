package com.towerbreak.towerbreakgame.presentation.ranks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.towerbreak.towerbreakgame.foundation.text.grouped
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.LevelCurve
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/** Rank ladder + lifetime personal bests (the Flutter `RankSheet`). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RanksScreen(
    onBack: () -> Unit,
    viewModel: RanksViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)

    val progression by viewModel.progression.collectAsStateWithLifecycle()
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BrandPalette.RibbonDeep,
        topBar = {
            TopAppBar(
                title = { Text("RANKS", style = GameType.label(size = 18)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = BrandPalette.Parchment)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPalette.Ribbon),
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                RankHeaderCard(rank = progression.rank, into = progression.intoRank, span = progression.rankSpan, fraction = progression.fractionThroughRank)
            }
            item {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BestStat(Icons.Rounded.Height, "Best floor", milestones.topHeight.toString(), Modifier.weight(1f))
                    BestStat(Icons.Rounded.Paid, "Best payout", milestones.topPayout.grouped(), Modifier.weight(1f))
                    BestStat(Icons.Rounded.LocalFireDepartment, "Best streak", milestones.topStreak.toString(), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text("RANK LADDER", style = GameType.label(size = 14, tint = BrandPalette.Bullion))
                Spacer(Modifier.height(4.dp))
            }
            items((1..LevelCurve.TOP_RANK).toList()) { rung ->
                LadderRow(
                    rung = rung,
                    achieved = rung <= progression.rank,
                    current = rung == progression.rank,
                    bounty = LevelCurve.bounty(rung),
                    gifts = LevelCurve.giftedSkinsAt(rung),
                )
            }
        }
    }
}

@Composable
private fun RankHeaderCard(rank: Int, into: Int, span: Int, fraction: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BrandPalette.Panel)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandPalette.Bullion),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = BrandPalette.RibbonDeep)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("RANK $rank", style = GameType.banner(size = 22, tint = BrandPalette.White))
                Text(
                    if (rank >= LevelCurve.TOP_RANK) "Max rank reached" else "$into / $span XP to next rank",
                    style = GameType.prose(size = 12, tint = BrandPalette.ParchmentFaded),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BrandPalette.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BrandPalette.Bullion),
            )
        }
    }
}

@Composable
private fun BestStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BrandPalette.Panel)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = BrandPalette.AzurePale, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = GameType.tally(size = 15, tint = BrandPalette.White))
        Text(label, style = GameType.prose(size = 10, tint = BrandPalette.ParchmentFaded))
    }
}

@Composable
private fun LadderRow(rung: Int, achieved: Boolean, current: Boolean, bounty: Int, gifts: List<Int>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (current) BrandPalette.Bullion.copy(alpha = 0.16f) else BrandPalette.Panel.copy(alpha = if (achieved) 1f else 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (achieved) BrandPalette.Moss.copy(alpha = 0.24f) else BrandPalette.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (achieved) {
                Text(rung.toString(), style = GameType.tally(size = 13, tint = BrandPalette.Moss))
            } else {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandPalette.ParchmentFaded, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Rank $rung" + if (current) " — current" else "",
                style = GameType.label(size = 13, tint = if (achieved) BrandPalette.White else BrandPalette.ParchmentFaded),
            )
            if (gifts.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, tint = BrandPalette.AzurePale, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New skin unlocked", style = GameType.prose(size = 11, tint = BrandPalette.AzurePale))
                }
            }
        }
        Text("+${bounty.grouped()}", style = GameType.tally(size = 13, tint = if (achieved) BrandPalette.Bullion else BrandPalette.ParchmentFaded))
    }
}
