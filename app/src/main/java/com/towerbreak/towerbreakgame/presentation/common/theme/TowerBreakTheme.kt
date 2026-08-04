package com.towerbreak.towerbreakgame.presentation.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.towerbreak.towerbreakgame.foundation.theme.BrandPalette

/**
 * App theme. The game paints its own chrome, so Material is only used for ripples
 * and system defaults — hence the deliberately small scheme.
 */
@Composable
fun TowerBreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = BrandPalette.Azure,
        secondary = BrandPalette.Caution,
        background = BrandPalette.Sky,
        surface = BrandPalette.Panel,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
