package com.towerbreak.towerbreakgame.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour the game paints with, grouped by *where* it appears.
 *
 * Fingerprint note: the Flutter original exposed these as a `Palette` abstract
 * class of `Color(0x..)` literals. Here they live as an `object` of Compose
 * [Color]s with role-based names, so the call sites read as `BrandPalette.sky`
 * and the theme wiring stays in one file.
 */
object BrandPalette {
    // Playfield sky.
    val Sky = Color(0xFF7FB5D6)
    val SkyHigh = Color(0xFF8FC6E4)
    val SkyLow = Color(0xFFBFE0EE)

    // Chrome: ribbons, panels, sheets.
    val Ribbon = Color(0xFF2B2B2D)
    val RibbonDeep = Color(0xFF1E1E20)
    val Panel = Color(0xF21F2230)
    val ParchmentPanel = Color(0xFFFFF6E2)

    // Interactive accents.
    val Azure = Color(0xFF2F7BE0)
    val AzureDeep = Color(0xFF1B58AC)
    val AzurePale = Color(0xFF63A4F2)

    val Caution = Color(0xFFF4B41A)
    val CautionDeep = Color(0xFFC8890A)

    val Moss = Color(0xFF49B265)
    val MossDeep = Color(0xFF2E8247)

    val Alarm = Color(0xFFE0533D)
    val Bullion = Color(0xFFFFD23F)

    // Type.
    val Parchment = Color(0xFFFFF6E2)
    val ParchmentFaded = Color(0xFFC9CAD6)
    val InkOnLight = Color(0xFF2A2233)

    // Neutrals.
    val White = Color(0xFFFFFFFF)
    val ShadowSoft = Color(0x8A000000)
    val ShadowMid = Color(0x99000000)
    val Hairline = Color(0x3DFFFFFF)
}
