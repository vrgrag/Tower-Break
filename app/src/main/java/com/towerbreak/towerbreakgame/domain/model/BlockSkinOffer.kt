package com.towerbreak.towerbreakgame.domain.model

/**
 * A block skin as the shop sells it (the Flutter `BrickOffer`). Skin 1 is the
 * free starter.
 */
data class BlockSkinOffer(
    val skin: Int,
    val displayName: String,
    val price: Int,
) {
    companion object {
        val catalogue: List<BlockSkinOffer> = listOf(
            BlockSkinOffer(1, "Cozy Wood", 0),
            BlockSkinOffer(2, "Sunny House", 800),
            BlockSkinOffer(3, "Log Cabin", 1500),
            BlockSkinOffer(4, "Brick Loft", 2500),
        )
    }
}
