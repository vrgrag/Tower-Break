package com.towerbreak.towerbreakgame.presentation.hub

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.towerbreak.towerbreakgame.core.theme.BrandPalette
import kotlin.math.PI
import kotlin.math.sin

/**
 * Slow-drifting gold sparkles behind the menu content. Purely decorative — one
 * `Canvas`, a dozen circles, one shared animation clock — so it is cheap enough
 * to run for the lifetime of the hub screen.
 */
@Composable
fun FloatingEmbers(modifier: Modifier = Modifier, count: Int = 18) {
    val infinite = rememberInfiniteTransition(label = "embers")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "ember-time",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        for (i in 0 until count) {
            // Golden-ratio spread gives an even, non-repeating scatter from one index.
            val seed = (i * 0.6180339887f) % 1f
            val speed = 0.55f + (i % 5) * 0.16f
            val phase = (time * speed + seed) % 1f
            val x = ((seed * 12.9898f) % 1f) * size.width
            val y = size.height * (1f - phase)
            val wobble = sin(phase * 2 * PI.toFloat() + seed * 10f) * size.width * 0.035f
            val radius = (1.4f + (i % 4)) * density
            val alpha = (sin(phase * PI.toFloat()) * 0.5f).coerceIn(0f, 0.5f)
            if (alpha <= 0f) continue
            drawCircle(
                color = BrandPalette.Bullion.copy(alpha = alpha),
                radius = radius,
                center = Offset(x + wobble, y),
            )
        }
    }
}

/**
 * A soft, breathing radial glow behind [content] — used to make the PLAY button
 * read as the one thing on screen worth tapping.
 */
@Composable
fun PulsingGlow(color: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "glow")
    val scale by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow-scale",
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow-alpha",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = glowAlpha }
                .background(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0f))),
                    CircleShape,
                ),
        )
        content()
    }
}
