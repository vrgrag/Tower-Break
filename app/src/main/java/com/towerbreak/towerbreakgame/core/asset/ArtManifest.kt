package com.towerbreak.towerbreakgame.core.asset

/**
 * Bundle paths, in one place, so no call site spells a literal (the Flutter
 * `ArtManifest`). Paths are relative to the Android `assets/` root.
 */
object ArtManifest {
    private const val WORLD = "gameplay"
    private const val INTERFACE = "ui"

    // Playfield art.
    const val SKY_BACKDROP = "$WORLD/bg_sky_asset.webp"
    const val SKYLINE = "$WORLD/start_bg_asset.webp"
    const val PEDESTAL = "$WORLD/start_block_asset.webp"
    const val CRANE_HOOK = "$WORLD/hook_asset.webp"
    const val PLATE_BLANK = "$WORLD/button_blank.png"
    const val PUFF = "$WORLD/cloud_asset_01.webp"
    const val PUFF_ALT = "$WORLD/cloud_asset_02.webp"

    const val BLOCK_SKIN_COUNT = 4
    fun block(skin: Int): String = "$WORLD/block_asset_0$skin.webp"
    fun everyBlock(): List<String> = (1..BLOCK_SKIN_COUNT).map(::block)

    /** Everything the renderer needs decoded before the first frame. */
    fun preloadable(): List<String> = buildList {
        add(SKY_BACKDROP); add(SKYLINE); add(PEDESTAL); add(CRANE_HOOK)
        add(PUFF); add(PUFF_ALT)
        addAll(everyBlock())
    }

    // Interface art.
    const val WORDMARK = "$INTERFACE/Game_Name.webp"
    const val HUB_BACKDROP = "$WORLD/start_bg_asset.webp"
    const val SPLASH_PORTRAIT = "$INTERFACE/Vertical_Loading_Screen.webp"
    const val SPLASH_LANDSCAPE = "$INTERFACE/Horizontal_Loading_Screen.webp"
}
