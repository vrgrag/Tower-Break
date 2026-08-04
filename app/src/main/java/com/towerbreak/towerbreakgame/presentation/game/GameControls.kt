package com.towerbreak.towerbreakgame.presentation.game

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.towerbreak.towerbreakgame.foundation.text.asMultiplier
import com.towerbreak.towerbreakgame.foundation.text.grouped
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.domain.model.GameTuning
import com.towerbreak.towerbreakgame.domain.model.HazardBand
import com.towerbreak.towerbreakgame.presentation.common.components.CandyButton
import com.towerbreak.towerbreakgame.presentation.common.components.CandySkin
import com.towerbreak.towerbreakgame.presentation.common.components.CautionPlate
import com.towerbreak.towerbreakgame.presentation.common.components.CautionStripe
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundHudState
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundStage

/**
 * The bottom control panel (the Flutter `ArenaControls`). Shows the stake
 * controls before a round and the live floor/cash-out controls during one, and
 * gets out of the way once the round is over so the verdict has the screen.
 */
@Composable
fun GameControls(
    hud: RoundHudState,
    stake: Int,
    balance: Int,
    band: HazardBand,
    onStake: (Int) -> Unit,
    onBuild: () -> Unit,
    onBank: () -> Unit,
    onBand: (HazardBand) -> Unit,
    onNudge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hud.stage.isOver) return

    // A block already in the air must not be double-released.
    val buildTap: (() -> Unit)? = if (hud.stage == RoundStage.PLUNGING) null else onBuild

    Column(modifier = modifier.fillMaxWidth()) {
        CautionStripe()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BrandPalette.Ribbon, BrandPalette.RibbonDeep)))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        ) {
            if (hud.stage == RoundStage.BETTING) {
                RiskRow(selected = band, onSelect = onBand)
                Spacer(Modifier.height(10.dp))
                StakeRow(stake = stake, ceiling = balance, onStake = onStake, onNudge = onNudge)
                Spacer(Modifier.height(10.dp))
                CautionPlate(onPressed = buildTap, heightDp = 70)
            } else {
                StoreyStrip(hud)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CandyButton(
                        label = "CASH OUT",
                        footnote = "${hud.potential.grouped()} FUN",
                        skin = CandySkin.MOSS,
                        height = 62.dp,
                        fontSize = 20,
                        fillWidth = true,
                        onPressed = if (hud.canBank) onBank else null,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        CautionPlate(onPressed = buildTap, heightDp = 62)
                    }
                }
            }
        }
    }
}

/** The four hazard bands as a segmented row (the Flutter `RiskRow`). */
@Composable
private fun RiskRow(selected: HazardBand, onSelect: (HazardBand) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        HazardBand.entries.forEach { band ->
            val active = band == selected
            val bg by animateColorAsState(if (active) band.tint else BrandPalette.Panel, label = "band-bg")
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg, RoundedCornerShape(14.dp))
                    .border(if (active) 2.dp else 1.dp, if (active) BrandPalette.White else BrandPalette.Hairline, RoundedCornerShape(14.dp))
                    .clickable { onSelect(band) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    band.label,
                    style = GameType.label(size = 13, tint = if (active) BrandPalette.White else BrandPalette.ParchmentFaded),
                )
            }
        }
    }
}

/** Pre-round stake controls (the Flutter `StakeRow`). */
@Composable
private fun StakeRow(stake: Int, ceiling: Int, onStake: (Int) -> Unit, onNudge: () -> Unit) {
    val step = when {
        stake < 100 -> 10
        stake < 1000 -> 50
        stake < 10000 -> 250
        else -> 1000
    }
    val affordable = ceiling >= GameTuning.STAKE_FLOOR
    fun bound(value: Int) = value.coerceIn(GameTuning.STAKE_FLOOR, if (affordable) ceiling else GameTuning.STAKE_FLOOR)
    val apply = { value: Int -> onNudge(); onStake(bound(value)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        CandyButton(
            label = "ALL IN",
            fontSize = 16,
            height = 54.dp,
            width = 92.dp,
            onPressed = if (affordable) ({ apply(ceiling) }) else null,
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandPalette.Panel)
                .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(16.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepKey(Icons.Rounded.Remove) { apply(stake - step) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stake.grouped(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = GameType.tally(size = 22, tint = BrandPalette.White))
                Text("BET", style = GameType.prose(size = 10, tint = BrandPalette.ParchmentFaded))
            }
            StepKey(Icons.Rounded.Add) { apply(stake + step) }
        }
        Spacer(Modifier.width(8.dp))
        CandyButton(label = "x2", fontSize = 18, height = 54.dp, width = 70.dp, onPressed = { apply(stake * 2) })
    }
}

@Composable
private fun StepKey(icon: androidx.compose.ui.graphics.vector.ImageVector, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(BrandPalette.White.copy(alpha = 0.1f))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = BrandPalette.White, modifier = Modifier.size(26.dp))
    }
}

/** Current floor count and the compounded payout multiplier (`StoreyStrip`). */
@Composable
private fun StoreyStrip(hud: RoundHudState) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrandPalette.Panel)
            .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Apartment, contentDescription = null, tint = BrandPalette.Bullion, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Floor ${hud.storey}", style = GameType.label(size = 15, tint = BrandPalette.White))
        Spacer(Modifier.width(14.dp))
        Text(hud.payoutMultiplier.asMultiplier(), style = GameType.tally(size = 18, tint = BrandPalette.Bullion))
    }
}

/** The running column of per-floor rolls down the right edge (`RollsColumn`). */
@Composable
fun RollsColumn(rolls: List<Double>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BrandPalette.Panel.copy(alpha = 0.78f))
            .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Results", style = GameType.prose(size = 11, tint = BrandPalette.ParchmentFaded))
        Spacer(Modifier.height(5.dp))
        rolls.take(6).forEach { RollChip(it) }
    }
}

@Composable
private fun RollChip(roll: Double) {
    // A roll under 1.0 shrinks the payout, so it reads as a loss.
    val tint = if (roll >= 1.0) BrandPalette.Moss else BrandPalette.Alarm
    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.22f))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(roll.asMultiplier(), style = GameType.tally(size = 13, tint = BrandPalette.White))
    }
}
