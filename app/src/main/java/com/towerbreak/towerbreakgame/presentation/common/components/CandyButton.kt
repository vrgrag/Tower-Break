package com.towerbreak.towerbreakgame.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette
import com.towerbreak.towerbreakgame.presentation.common.theme.GameType

/**
 * The five colourways of the glossy button (the Flutter `CandySkin`), each
 * carrying its own gradient and rim so there is no palette lookup to keep in
 * sync.
 */
enum class CandySkin(val top: Color, val bottom: Color, val rim: Color) {
    AZURE(Color(0xFF63A8F5), Color(0xFF1F66C9), Color(0xFF12407F)),
    MOSS(Color(0xFF6BD089), Color(0xFF2E9A4F), Color(0xFF1C5E31)),
    EMBER(Color(0xFFF08568), Color(0xFFD0432B), Color(0xFF822316)),
    AMBER(Color(0xFFFFD66B), Color(0xFFF1A81C), Color(0xFF9A6708)),
    SLATE(Color(0xFF4A4D5C), Color(0xFF2B2D38), Color(0xFF15161D)),
    VIOLET(Color(0xFFA78BFA), Color(0xFF7C3AED), Color(0xFF4C1D95)),
}

/**
 * The chunky candy-style action button (the Flutter `CandyButton`): a vertical
 * gradient, a dark rim and a sheen across the top for a 3D pop.
 */
@Composable
fun CandyButton(
    label: String,
    onPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    icon: ImageVector? = null,
    skin: CandySkin = CandySkin.AZURE,
    width: Dp? = null,
    height: Dp = 58.dp,
    fontSize: Int = 20,
    fillWidth: Boolean = false,
) {
    val radius = height * 0.3f
    Pressable(onPressed = onPressed, modifier = modifier) { _ ->
        Box(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else if (width != null) Modifier.width(width) else Modifier)
                .height(height)
                .clip(RoundedCornerShape(radius))
                .background(Brush.verticalGradient(listOf(skin.top, skin.bottom)))
                .border(2.5.dp, skin.rim, RoundedCornerShape(radius))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Sheen: glassy highlight sitting just inside the top edge.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(height * 0.4f)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(radius * 0.8f))
                    .background(
                        Brush.verticalGradient(
                            listOf(BrandPalette.White.copy(alpha = 0.55f), BrandPalette.White.copy(alpha = 0f)),
                        ),
                    ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = BrandPalette.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = GameType.label(size = fontSize, tint = BrandPalette.White),
                    )
                    if (footnote != null) {
                        Text(
                            text = footnote,
                            style = GameType.prose(size = (fontSize * 0.62).toInt(), tint = BrandPalette.White.copy(alpha = 0.92f)),
                        )
                    }
                }
            }
        }
    }
}
