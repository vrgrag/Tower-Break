package com.towerbreak.towerbreakgame.presentation.common.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.towerbreak.towerbreakgame.core.theme.BrandPalette

/**
 * The four type roles used across the game (the Flutter `Lettering`).
 *
 * Fingerprint / porting note: the original pulled "Luckiest Guy" (display) and
 * "Fredoka" (rounded) from Google Fonts. To keep this project self-contained the
 * families default to the platform faces with matching weights; drop the two
 * TTFs into `res/font` and swap [display]/[rounded] to reproduce the exact look.
 */
object GameType {
    // Swap these to `FontFamily(Font(R.font.luckiest_guy))` etc. once bundled.
    private val display = FontFamily.Default
    private val rounded = FontFamily.Default

    /** Chunky poster type for screen titles and the big multiplier headline. */
    fun banner(size: Int = 40, tint: androidx.compose.ui.graphics.Color = BrandPalette.Parchment) = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.Black,
        fontSize = size.sp,
        color = tint,
        letterSpacing = 1.0.sp,
        shadow = Shadow(color = BrandPalette.ShadowSoft, offset = Offset(2f, 3f), blurRadius = 6f),
    )

    /** Bold rounded type for button captions, list rows and card headers. */
    fun label(size: Int = 22, tint: androidx.compose.ui.graphics.Color = BrandPalette.Parchment) = TextStyle(
        fontFamily = rounded,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp,
        color = tint,
        letterSpacing = 0.3.sp,
    )

    /** Regular rounded type for descriptions and secondary captions. */
    fun prose(size: Int = 15, tint: androidx.compose.ui.graphics.Color = BrandPalette.Parchment) = TextStyle(
        fontFamily = rounded,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        color = tint,
    )

    /** Tabular-feeling type for coin balances, stakes and multipliers. */
    fun tally(size: Int = 22, tint: androidx.compose.ui.graphics.Color = BrandPalette.Parchment) = TextStyle(
        fontFamily = rounded,
        fontWeight = FontWeight.Bold,
        fontSize = size.sp,
        color = tint,
        letterSpacing = 0.5.sp,
    )
}
