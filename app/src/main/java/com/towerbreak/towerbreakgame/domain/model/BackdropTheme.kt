package com.towerbreak.towerbreakgame.domain.model

import androidx.compose.ui.graphics.Color

/**
 * A purchasable playfield backdrop: the two colours the sky gradient runs
 * between plus how the player unlocks it (the Flutter `SkySkin`).
 */
enum class BackdropTheme(
    val slot: Int,
    val displayName: String,
    val high: Color,
    val low: Color,
    /** 0 marks the free default. */
    val price: Int,
    val unlockLevel: Int,
) {
    DAYLIGHT(0, "Daylight", Color(0xFF8FC6E4), Color(0xFFD3ECF5), 0, 0),
    SUNSET(1, "Sunset", Color(0xFFF7B267), Color(0xFFF4845F), 1500, 0),
    DUSK(2, "Dusk", Color(0xFF5B6CA8), Color(0xFFB18FCF), 3000, 3),
    MIDNIGHT(3, "Midnight", Color(0xFF1E2A4A), Color(0xFF3C5078), 6000, 6);

    companion object {
        /** Falls back to the free default so a corrupt slot never blanks the sky. */
        fun bySlot(slot: Int): BackdropTheme =
            entries.firstOrNull { it.slot == slot } ?: DAYLIGHT
    }
}
