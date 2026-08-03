package com.towerbreak.towerbreakgame.presentation.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.towerbreak.towerbreakgame.core.theme.BrandPalette

/**
 * The yellow-and-black diagonal stripe that trims the status ribbon and the
 * bottom control panel (the Flutter `CautionStripe`).
 */
@Composable
fun CautionStripe(modifier: Modifier = Modifier, heightDp: Int = 6) {
    val bar = Color(0xFF222226)
    val slant = 14f
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
    ) {
        drawRect(BrandPalette.Caution)
        val h = size.height
        var x = -h
        while (x < size.width + h) {
            val path = Path().apply {
                moveTo(x, 0f)
                lineTo(x + slant, 0f)
                lineTo(x + slant - h, h)
                lineTo(x - h, h)
                close()
            }
            drawPath(path, bar)
            x += slant * 2
        }
    }
}
