package com.towerbreak.towerbreakgame.presentation.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Shared press behaviour for the game's chunky buttons (the Flutter `Pressable`):
 * a brief scale-down while held and a flat fade when disabled.
 *
 * Fingerprint note: Flutter used a `StatefulWidget` + `GestureDetector` +
 * `AnimatedScale`. Here it is a stateless composable driven by an
 * [MutableInteractionSource]; the "held" flag is derived reactively rather than
 * pushed through `setState`.
 */
@Composable
fun Pressable(
    onPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
    heldScale: Float = 0.96f,
    disabledOpacity: Float = 0.5f,
    content: @Composable (held: Boolean) -> Unit,
) {
    val enabled = onPressed != null
    val interaction = remember { MutableInteractionSource() }
    val held by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (held && enabled) heldScale else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "press-scale",
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (enabled) 1f else disabledOpacity)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) { onPressed?.invoke() },
    ) {
        content(held)
    }
}
