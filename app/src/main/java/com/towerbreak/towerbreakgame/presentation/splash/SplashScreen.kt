package com.towerbreak.towerbreakgame.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.RotationLock
import com.towerbreak.towerbreakgame.presentation.common.ScreenAxis
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType
import androidx.compose.runtime.LaunchedEffect
import android.content.res.Configuration

/**
 * Cold-start splash (the Flutter `BootStage`).
 *
 * Two modes, because the game is not always the first thing to draw. On a plain
 * cold start this owns the boot: the bar runs its full length and [onReady] waits
 * for both the warm-up and that animation.
 *
 * When [chained] is set, the launcher already ran a loading screen on the very
 * same artwork and its bar already reached the end. Replaying the bar there makes
 * one boot look like two, so this holds the identical frame — which makes the
 * hand-off invisible — and leaves the moment the warm-up lands. The warm-up
 * itself is never skipped: the arena must not read un-hydrated state.
 */
@Composable
fun SplashScreen(
    onReady: () -> Unit,
    chained: Boolean = false,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    RotationLock(ScreenAxis.ANY)

    val warmedUp by viewModel.warmedUp.collectAsStateWithLifecycle()
    val progress = remember { Animatable(0f) }
    var barDone by remember { mutableStateOf(false) }

    LaunchedEffect(chained) {
        if (chained) return@LaunchedEffect
        progress.animateTo(1f, tween(BAR_MS, easing = LinearEasing))
        barDone = true
    }
    LaunchedEffect(warmedUp, barDone) {
        if (warmedUp && (chained || barDone)) onReady()
    }

    val wide = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val artwork = if (wide) {
        "file:///android_asset/ui/Horizontal_Loading_Screen.webp"
    } else {
        "file:///android_asset/ui/Vertical_Loading_Screen.webp"
    }

    Box(Modifier.fillMaxSize().background(BrandPalette.RibbonDeep)) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (!chained) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (wide) 16.dp else 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoadBar(
                    progress = progress.value,
                    modifier = Modifier.fillMaxWidth(if (wide) 0.34f else 0.62f),
                )
                Spacer(Modifier.height(10.dp))
                Text("Loading…", style = GameType.prose(size = 13, tint = BrandPalette.White.copy(alpha = 0.7f)))
            }
        }
    }
}

private const val BAR_MS = 2400

@Composable
private fun LoadBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, BrandPalette.Hairline, RoundedCornerShape(10.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Brush.horizontalGradient(listOf(BrandPalette.Caution, BrandPalette.Bullion))),
        )
    }
}
