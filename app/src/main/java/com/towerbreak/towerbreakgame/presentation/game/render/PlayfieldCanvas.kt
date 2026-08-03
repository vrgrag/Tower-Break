package com.towerbreak.towerbreakgame.presentation.game.render

import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.towerbreak.towerbreakgame.presentation.game.engine.PlayfieldConfig
import com.towerbreak.towerbreakgame.presentation.game.engine.PlayfieldSnapshot
import kotlin.math.max
import kotlin.random.Random
import android.graphics.Canvas as AndroidCanvas

/**
 * Paints the whole playfield onto a Compose [Canvas].
 *
 * Fingerprint / architecture note: the Flutter build used a `CustomPainter` and
 * a list of `SceneLayer` objects driven by `repaint: sim`. Here the render is a
 * plain top-down sequence of private draw functions over the native canvas —
 * there is no layer polymorphism, and the redraw is driven by Compose observing
 * the [snapshot]. The drawing maths (units-per-world-span projection, contact
 * shadows, parallax clouds) is preserved because it is what makes the scene feel
 * identical.
 */
@Composable
fun PlayfieldCanvas(
    snapshot: PlayfieldSnapshot,
    textures: PlayfieldTextures,
    skyHigh: Color,
    skyLow: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (!textures.ready) return@Canvas
        drawIntoCanvas { canvas ->
            renderScene(
                canvas = canvas.nativeCanvas,
                widthPx = size.width,
                heightPx = size.height,
                snap = snapshot,
                tex = textures,
                skyHigh = skyHigh.toArgb(),
                skyLow = skyLow.toArgb(),
            )
        }
    }
}

/** Maps world coordinates onto canvas pixels for one frame (the `Projector`). */
private class SceneProjector(
    val width: Float,
    val height: Float,
    val lensY: Double,
    val jitterX: Float,
    val jitterY: Float,
) {
    val unit: Float = (width / PlayfieldConfig.WORLD_SPAN).toFloat()
    fun mapX(worldX: Double): Float = width / 2 + (worldX * unit).toFloat() + jitterX
    fun mapY(worldY: Double): Float = height / 2 + ((worldY - lensY) * unit).toFloat() + jitterY
}

private val shake = Random.Default

private fun renderScene(
    canvas: AndroidCanvas,
    widthPx: Float,
    heightPx: Float,
    snap: PlayfieldSnapshot,
    tex: PlayfieldTextures,
    skyHigh: Int,
    skyLow: Int,
) {
    val unit = (widthPx / PlayfieldConfig.WORLD_SPAN).toFloat()
    val jitter = if (snap.tremor > 0) {
        val mag = snap.tremor.toFloat() * unit * 0.5f
        Pair((shake.nextFloat() - 0.5f) * mag, (shake.nextFloat() - 0.5f) * mag)
    } else Pair(0f, 0f)

    val proj = SceneProjector(widthPx, heightPx, snap.lensY, jitter.first, jitter.second)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    drawSky(canvas, proj, skyHigh, skyLow, paint)
    drawClouds(canvas, proj, snap, tex, paint)
    drawSkyline(canvas, proj, tex, paint)
    drawGround(canvas, proj, paint)
    drawPedestal(canvas, proj, tex, paint)
    snap.seated.forEach { drawBlock(canvas, proj, tex.block(it.skin), it.centreX, it.centreY, it.span, it.rise, it.tilt, paint) }
    snap.falling?.let { drawBlock(canvas, proj, tex.block(it.skin), it.x, it.y, it.span, it.rise, it.angle, paint) }
    drawGantry(canvas, proj, snap, tex, paint)
    drawParticles(canvas, proj, snap, paint)
}

private fun drawSky(canvas: AndroidCanvas, proj: SceneProjector, high: Int, low: Int, paint: Paint) {
    paint.shader = LinearGradient(
        proj.width / 2, 0f, proj.width / 2, proj.height,
        high, low, Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, proj.width, proj.height, paint)
    paint.shader = null
}

private fun drawClouds(canvas: AndroidCanvas, proj: SceneProjector, snap: PlayfieldSnapshot, tex: PlayfieldTextures, paint: Paint) {
    val sprites = listOf(tex.puff, tex.puffAlt, tex.puff)
    val sink = (-proj.lensY * proj.unit * 0.1).toFloat()
    paint.alpha = 235
    sprites.forEachIndexed { i, sprite ->
        val w = proj.width * (0.42f + i * 0.08f)
        val h = w * sprite.height / sprite.width
        val speed = 0.5f + i * 0.25f
        val wrap = proj.width + w + 80f
        var x = ((snap.cloudDrift * speed * proj.width).toFloat() + i * wrap * 0.41f) % wrap - w
        val y = proj.height * (0.1f + i * 0.18f) + sink * (0.4f + i * 0.2f)
        blit(canvas, sprite, RectF(x, y, x + w, y + h), paint)
    }
    paint.alpha = 255
}

private fun drawSkyline(canvas: AndroidCanvas, proj: SceneProjector, tex: PlayfieldTextures, paint: Paint) {
    val sprite = tex.skyline
    val span = PlayfieldConfig.WORLD_SPAN * 1.06
    val rise = span * sprite.height / sprite.width
    val baseY = PlayfieldConfig.GROUND_LINE
    val left = proj.mapX(-span / 2)
    val top = proj.mapY(baseY - rise)
    val right = proj.mapX(span / 2)
    val bottom = proj.mapY(baseY)
    blit(canvas, sprite, RectF(left, top, right, bottom), paint)
}

private fun drawGround(canvas: AndroidCanvas, proj: SceneProjector, paint: Paint) {
    val surface = proj.mapY(PlayfieldConfig.GROUND_LINE)
    val bottom = surface + proj.height * 2
    if (bottom < 0 || surface > proj.height) return
    paint.shader = LinearGradient(
        0f, surface, 0f, surface + proj.height * 0.5f,
        0xFF5A4632.toInt(), 0xFF2E2114.toInt(), Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, surface, proj.width, bottom, paint)
    paint.shader = null
    paint.color = 0xFF6E573E.toInt()
    canvas.drawRect(0f, surface, proj.width, surface + 3f, paint)
}

private fun drawPedestal(canvas: AndroidCanvas, proj: SceneProjector, tex: PlayfieldTextures, paint: Paint) {
    val crown = PlayfieldConfig.pedestalCrown
    val glowCx = proj.mapX(0.0)
    val glowCy = proj.mapY(crown + PlayfieldConfig.PEDESTAL_RISE * 0.4)
    val glowRadius = (PlayfieldConfig.PEDESTAL_SPAN * proj.unit * 0.62).toFloat()
    paint.shader = RadialGradient(
        glowCx, glowCy, glowRadius,
        0x66FFE9A8, 0x00FFE9A8, Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(glowCx, glowCy, glowRadius, paint)
    paint.shader = null

    val left = proj.mapX(-PlayfieldConfig.PEDESTAL_SPAN / 2)
    val top = proj.mapY(crown)
    val right = proj.mapX(PlayfieldConfig.PEDESTAL_SPAN / 2)
    val bottom = proj.mapY(PlayfieldConfig.GROUND_LINE)
    blit(canvas, tex.pedestal, RectF(left, top, right, bottom), paint)
}

private fun drawGantry(canvas: AndroidCanvas, proj: SceneProjector, snap: PlayfieldSnapshot, tex: PlayfieldTextures, paint: Paint) {
    val hook = tex.craneHook
    val x = snap.craneX
    val hookCentreY = snap.lensY - PlayfieldConfig.HOOK_LIFT_ABOVE_LENS
    val hookRise = PlayfieldConfig.HOOK_RISE
    val hookSpan = hookRise * hook.width / hook.height
    val hookCx = proj.mapX(x)
    val hookCy = proj.mapY(hookCentreY)

    if (snap.carriesBlock) {
        val blockCentreY = snap.hangCentreY
        val blockCrownY = blockCentreY - snap.carriedRise / 2
        val chain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF20242B.toInt()
            strokeWidth = max(2f, 0.07f * proj.unit)
            strokeCap = Paint.Cap.ROUND
        }
        val inset = PlayfieldConfig.BLOCK_SPAN * 0.12
        val baseX = proj.mapX(x)
        val baseY = proj.mapY(hookCentreY + hookRise / 2)
        canvas.drawLine(baseX, baseY, proj.mapX(x - PlayfieldConfig.BLOCK_SPAN / 2 + inset), proj.mapY(blockCrownY), chain)
        canvas.drawLine(baseX, baseY, proj.mapX(x + PlayfieldConfig.BLOCK_SPAN / 2 - inset), proj.mapY(blockCrownY), chain)

        drawBlock(canvas, proj, tex.block(snap.carriedSkin), x, blockCentreY, PlayfieldConfig.BLOCK_SPAN, snap.carriedRise, 0.0, paint)
    }

    // Drawn last: the artwork's own cables run up off the top of the screen.
    val w = (hookSpan * proj.unit).toFloat()
    val h = (hookRise * proj.unit).toFloat()
    blit(canvas, hook, RectF(hookCx - w / 2, hookCy - h / 2, hookCx + w / 2, hookCy + h / 2), paint)
}

private fun drawParticles(canvas: AndroidCanvas, proj: SceneProjector, snap: PlayfieldSnapshot, paint: Paint) {
    snap.particles.forEach { p ->
        val fade = p.remaining
        val radius = (p.span * proj.unit * (if (p.shrinks) fade else 1.0)).toFloat()
        if (radius <= 0f) return@forEach
        val alpha = (if (p.smoky) fade * 0.85 else fade).coerceIn(0.0, 1.0)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.tint.toArgb()
            this.alpha = (alpha * 255).toInt()
            if (p.smoky) maskFilter = BlurMaskFilter(radius * 0.5f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(proj.mapX(p.x), proj.mapY(p.y), radius, dot)
    }
}

/**
 * Draws one block sprite with its soft contact shadow (the Flutter `paintBrick`).
 * Shared by the stack, the falling block and the carried block.
 */
private fun drawBlock(
    canvas: AndroidCanvas,
    proj: SceneProjector,
    bitmap: ImageBitmap,
    centreX: Double,
    centreY: Double,
    span: Double,
    rise: Double,
    angleRadians: Double,
    paint: Paint,
) {
    val ax = proj.mapX(centreX)
    val ay = proj.mapY(centreY)
    val w = (span * proj.unit).toFloat()
    val h = (rise * proj.unit).toFloat()

    canvas.save()
    canvas.translate(ax, ay)
    if (angleRadians != 0.0) canvas.rotate(Math.toDegrees(angleRadians).toFloat())

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x1F000000 // black @ 12%
        maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
    }
    val shadowRect = RectF(-w * 0.46f, h * 0.42f - h * 0.09f, w * 0.46f, h * 0.42f + h * 0.09f)
    canvas.drawRoundRect(shadowRect, h * 0.09f, h * 0.09f, shadow)

    val android = bitmap.asAndroidBitmap()
    canvas.drawBitmap(
        android,
        Rect(0, 0, android.width, android.height),
        RectF(-w / 2, -h / 2, w / 2, h / 2),
        paint,
    )
    canvas.restore()
}

private fun blit(canvas: AndroidCanvas, image: ImageBitmap, dst: RectF, paint: Paint) {
    val bmp = image.asAndroidBitmap()
    canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, paint)
}
