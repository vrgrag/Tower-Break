package com.towerbreak.towerbreakgame.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.towerbreak.towerbreakgame.core.text.asMultiplier
import com.towerbreak.towerbreakgame.core.text.grouped
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.components.CandyButton
import com.towerbreak.towerbreakgame.presentation.common.components.CandySkin
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundHudState
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundStage
import kotlin.math.pow
import kotlin.math.sin

/**
 * The transient multiplier headline (the Flutter `HeadlineFlash`). Pops in with
 * an elastic scale when a floor seats (gold) or the round collapses (red `x0`),
 * holds, then fades out on its own.
 */
@Composable
fun HeadlineFlash(hud: RoundHudState, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var text by remember { mutableStateOf("") }
    var tint by remember { mutableStateOf(BrandPalette.Bullion) }
    var shownStorey by remember { mutableIntStateOf(0) }
    var lastStage by remember { mutableStateOf(RoundStage.BETTING) }

    LaunchedEffect(hud.stage, hud.storey) {
        var fire = false
        when {
            hud.stage == RoundStage.BETTING -> shownStorey = 0
            hud.stage == RoundStage.COLLAPSED && lastStage != RoundStage.COLLAPSED -> {
                text = "x0"; tint = BrandPalette.Alarm; fire = true
            }
            (hud.stage == RoundStage.SWINGING || hud.stage == RoundStage.PLUNGING) &&
                hud.storey != shownStorey && hud.storey >= 1 -> {
                shownStorey = hud.storey
                text = hud.storeyMultiplier.asMultiplier()
                tint = BrandPalette.Bullion
                fire = true
            }
        }
        lastStage = hud.stage
        if (fire) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1300, easing = LinearEasing))
        }
    }

    val t = progress.value
    if (t <= 0f || t >= 1f) return

    val popIn = (t / POP_SHARE).coerceIn(0f, 1f)
    val scale = elasticOut(popIn) * 0.5f + 0.5f
    val opacity = if (t < HOLD_UNTIL) 1f else (1 - (t - HOLD_UNTIL) / FADE_SHARE).coerceIn(0f, 1f)

    Box(modifier = modifier) {
        Text(
            text = text,
            style = GameType.banner(size = 76, tint = tint),
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .alpha(opacity),
        )
    }
}

private const val POP_SHARE = 0.32f
private const val HOLD_UNTIL = 0.68f
private const val FADE_SHARE = 0.32f

/** Flutter's `Curves.elasticOut`, ported directly. */
private fun elasticOut(t: Float): Float {
    if (t == 0f || t == 1f) return t
    val period = 0.4f
    val s = period / 4f
    return (2f.pow(-10f * t) * sin((t - s) * (2f * Math.PI.toFloat()) / period) + 1f)
}

/** The end-of-round panel, shown a beat after the headline lands (`RoundVerdict`). */
@Composable
fun RoundVerdict(
    banked: Boolean,
    storey: Int,
    multiplier: Double,
    amount: Int,
    onReplay: () -> Unit,
    onLeave: () -> Unit,
) {
    val accent = if (banked) BrandPalette.Moss else BrandPalette.Alarm
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2E40), Color(0xFF1A1C28))))
                .border(2.5.dp, accent, RoundedCornerShape(26.dp))
                .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (banked) Icons.Rounded.EmojiEvents else Icons.Rounded.HeartBroken,
                contentDescription = null,
                tint = if (banked) BrandPalette.Bullion else BrandPalette.Alarm,
                modifier = Modifier.height(56.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(if (banked) "CASHED OUT!" else "BUSTED", style = GameType.banner(size = 30))
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (banked) "Floor $storey  •  ${multiplier.asMultiplier()}" else "You reached floor $storey",
                style = GameType.prose(size = 15, tint = BrandPalette.ParchmentFaded),
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "${if (banked) "+" else "-"}${amount.grouped()} FUN",
                    style = GameType.tally(size = 26, tint = if (banked) BrandPalette.Bullion else BrandPalette.White),
                )
            }
            Spacer(Modifier.height(18.dp))
            CandyButton(label = "PLAY AGAIN", height = 60.dp, fontSize = 20, fillWidth = true, onPressed = onReplay)
            Spacer(Modifier.height(10.dp))
            CandyButton(label = "HOME", skin = CandySkin.SLATE, height = 52.dp, fontSize = 17, fillWidth = true, onPressed = onLeave)
        }
    }
}

/** The gold banner used for rank-ups and "not enough FUN" nudges (`ToastChip`). */
@Composable
fun ToastChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(BrandPalette.Bullion, Color(0xFFE0A11A))))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = GameType.label(size = 15, tint = Color(0xFF5A3E00)),
        )
    }
}
