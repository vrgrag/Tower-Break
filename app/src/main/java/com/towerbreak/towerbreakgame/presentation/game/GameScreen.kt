package com.towerbreak.towerbreakgame.presentation.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.components.StatusRibbon
import com.towerbreak.towerbreakgame.presentation.game.engine.RoundStage
import com.towerbreak.towerbreakgame.presentation.game.render.PlayfieldCanvas

/**
 * The playable round screen (the Flutter `ArenaStage` + `_ArenaBody`). It is a
 * pure function of the ViewModel's flows: the canvas, HUD and overlays each
 * subscribe to the slice of state they need.
 */
@Composable
fun GameScreen(
    onLeave: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.UPRIGHT)

    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val snapshot by viewModel.playfield.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val rank by viewModel.rank.collectAsStateWithLifecycle()
    val backdrop by viewModel.backdrop.collectAsStateWithLifecycle()
    val band by viewModel.band.collectAsStateWithLifecycle()
    val stake by viewModel.stake.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val leave = {
        viewModel.onLeave()
        onLeave()
    }
    BackHandler(onBack = leave)

    // The game loop. withFrameNanos is valid here because a LaunchedEffect
    // coroutine carries the Compose MonotonicFrameClock; it also auto-pauses when
    // the composable leaves or the window stops producing frames.
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0.0 else (now - last) / 1_000_000_000.0
            last = now
            viewModel.advance(dt)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backdrop.high, backdrop.low))),
    ) {
        if (!ready) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = BrandPalette.White,
            )
            return@Box
        }

        // Tapping the field releases a block while the gantry swings.
        PlayfieldCanvas(
            snapshot = snapshot,
            textures = remember { viewModel.textures },
            skyHigh = backdrop.high,
            skyLow = backdrop.low,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (hud.stage == RoundStage.SWINGING) viewModel.onBuild() },
        )

        StatusRibbon(
            coins = balance,
            rank = rank,
            leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onLeading = leave,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        HeadlineFlash(
            hud = hud,
            modifier = Modifier.align(BiasAlignment(0f, -0.46f)),
        )

        if (hud.rolls.isNotEmpty()) {
            RollsColumn(
                rolls = hud.rolls,
                modifier = Modifier
                    .align(BiasAlignment(0.96f, -0.05f))
                    .padding(end = 6.dp),
            )
        }

        GameControls(
            hud = hud,
            stake = stake,
            balance = balance,
            band = band,
            onStake = viewModel::onStakeChange,
            onBuild = viewModel::onBuild,
            onBank = viewModel::onBank,
            onBand = viewModel::onChooseBand,
            onNudge = viewModel::onNudge,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (hud.resultReady && (hud.stage == RoundStage.BANKED || hud.stage == RoundStage.COLLAPSED)) {
            val banked = hud.stage == RoundStage.BANKED
            RoundVerdict(
                banked = banked,
                storey = hud.storey,
                multiplier = hud.payoutMultiplier,
                amount = if (banked) hud.potential else hud.stake,
                onReplay = viewModel::onReplay,
                onLeave = leave,
            )
        }

        toast?.let {
            ToastChip(text = it, modifier = Modifier.align(BiasAlignment(0f, -0.55f)))
        }
    }
}
