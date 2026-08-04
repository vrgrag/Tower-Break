package com.towerbreak.towerbreakgame.presentation.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * The gold-and-black BUILD bar (the Flutter `CautionPlate`). Uses the *blank*
 * plate artwork (loaded from `assets/`) and draws the caption on top so the
 * lettering never stretches even when the plate spans the whole width.
 */
@Composable
fun CautionPlate(
    onPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String = "BUILD",
    heightDp: Int = 64,
) {
    val fontSize = (heightDp * 0.42).toInt().coerceIn(18, 30)
    Pressable(onPressed = onPressed, modifier = modifier, heldScale = 0.97f, disabledOpacity = 0.55f) { _ ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "file:///android_asset/gameplay/button_blank.png",
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = label,
                style = GameType.label(size = fontSize, tint = BrandPalette.White).copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp,
                ),
            )
        }
    }
}
