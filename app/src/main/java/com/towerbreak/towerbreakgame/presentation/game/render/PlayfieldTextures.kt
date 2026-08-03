package com.towerbreak.towerbreakgame.presentation.game.render

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.towerbreak.towerbreakgame.core.asset.ArtManifest
import com.towerbreak.towerbreakgame.presentation.game.engine.BlockMetrics
import com.towerbreak.towerbreakgame.presentation.game.engine.PlayfieldConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decodes every playfield image once, up front, so the canvas can draw
 * synchronously each frame (the Flutter `SpriteVault`). Also precomputes the
 * clamped aspect ratio that decides how tall each block skin renders, and so
 * doubles as the engine's [BlockMetrics] source.
 *
 * Fingerprint note: exposes Compose [ImageBitmap]s (not `dart:ui.Image`) and
 * bundles the "how tall is this skin" maths behind the [BlockMetrics] interface
 * the pure engine depends on — the engine never sees a bitmap.
 */
@Singleton
class PlayfieldTextures @Inject constructor(
    @ApplicationContext private val context: Context,
) : BlockMetrics {

    private val decoded = mutableMapOf<String, ImageBitmap>()
    private val blockAspect = mutableMapOf<Int, Double>()

    @Volatile
    var ready: Boolean = false
        private set

    fun image(path: String): ImageBitmap =
        decoded[path] ?: error("Texture not decoded: $path")

    val skyBackdrop: ImageBitmap get() = image(ArtManifest.SKY_BACKDROP)
    val skyline: ImageBitmap get() = image(ArtManifest.SKYLINE)
    val pedestal: ImageBitmap get() = image(ArtManifest.PEDESTAL)
    val craneHook: ImageBitmap get() = image(ArtManifest.CRANE_HOOK)
    val puff: ImageBitmap get() = image(ArtManifest.PUFF)
    val puffAlt: ImageBitmap get() = image(ArtManifest.PUFF_ALT)
    fun block(skin: Int): ImageBitmap = image(ArtManifest.block(skin))

    override fun blockRise(skin: Int): Double =
        PlayfieldConfig.BLOCK_SPAN * (blockAspect[skin] ?: 1.0)

    /** Idempotent — safe to call from both the splash and the arena. */
    suspend fun warmUp() {
        if (ready) return
        withContext(Dispatchers.IO) {
            ArtManifest.preloadable().forEach { path ->
                decoded[path] = decode(path)
            }
            for (skin in 1..ArtManifest.BLOCK_SKIN_COUNT) {
                val bmp = block(skin)
                val aspect = (bmp.height.toDouble() / bmp.width)
                    .coerceIn(PlayfieldConfig.BLOCK_ASPECT_FLOOR, PlayfieldConfig.BLOCK_ASPECT_CEIL)
                blockAspect[skin] = aspect
            }
        }
        ready = true
    }

    private fun decode(path: String): ImageBitmap =
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
}
