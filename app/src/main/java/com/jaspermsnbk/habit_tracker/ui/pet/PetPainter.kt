package com.jaspermsnbk.habit_tracker.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Colours that stay the same in light and dark mode, so a pet always looks like itself. */
internal object PetColors {
    val Ink = Color(0xFF1F2A44)
    val White = Color.White
    val Blush = Color(0xFFFF8FA3)
    val Tongue = Color(0xFFFF8FA3)
    val Tear = Color(0xFF8FD3F4)
    val EggShell = Color(0xFFF6F3EE)
    val Red = Color(0xFFE5484D)
    val Leaf = Color(0xFF4CAF7A)
    val LeafStem = Color(0xFF3E8E5E)
    val Aura = Color(0xFF0F6B66)
    val Sparkle = Color(0xFF2FA89F)
    val Book = Color(0xFF2F7D79)
    val BookPage = Color(0xFFF1F7F6)
    val Shadow = Color(0x1F000000)
}

/** The colours that make up one species. */
internal class Palette(val body: Color, val dark: Color, val belly: Color)

internal fun Species.palette(): Palette = when (this) {
    Species.Mochi -> Palette(Color(0xFF9DB4FF), Color(0xFF6F8BE8), Color(0xFFE6EDFF))
    Species.Pip -> Palette(Color(0xFFF7C948), Color(0xFFE39B1C), Color(0xFFFFF1BF))
    Species.Bun -> Palette(Color(0xFFF7B7A3), Color(0xFFE8826C), Color(0xFFFFE6DB))
    Species.Shell -> Palette(Color(0xFF8ED6A8), Color(0xFF3E9E6B), Color(0xFFE4F6D9))
}

/**
 * Draws [look] into a 200 x 200 space starting at the origin. Callers scale and position the
 * drawing; [wave] (0..1) is a gentle idle sway, and [zPhase] (0..1) drives the sleep "z"s.
 */
internal fun DrawScope.drawPet(look: PetLook, wave: Float = 0f, zPhase: Float = 0f) {
    if (look.mood == ArtMood.Dead) {
        drawGrave(look.species)
        return
    }
    if (look.stage == 0) {
        drawEgg(look.species, wave)
        return
    }
    val g = petGeometry(look.species, look.stage)
    val palette = look.species.palette()
    drawGroundShadow(g.cx, g.w0 * 1.05f)
    drawOutfitBack(look.outfit, g)
    // The whole body breathes together, hinged on the ground.
    val squash = 1f + .015f * wave
    scale(1f / squash, squash, pivot = Offset(g.cx, GROUND_Y)) {
        when (look.species) {
            Species.Mochi -> drawMochi(look, g, palette)
            Species.Pip -> drawPip(look, g, palette)
            Species.Bun -> drawBun(look, g, palette)
            Species.Shell -> drawShell(look, g, palette)
        }
        drawFace(look, g)
        drawOutfitFront(look.outfit, g)
    }
    if (look.mood == ArtMood.Sleep) drawSleepZs(g, zPhase)
}

// ---------------------------------------------------------------- shared helpers

internal fun DrawScope.oval(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float = 1f) =
    drawOval(color, Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry), alpha)

internal fun DrawScope.rotatedOval(
    color: Color,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    degrees: Float,
    alpha: Float = 1f,
) = rotate(degrees, Offset(cx, cy)) { oval(color, cx, cy, rx, ry, alpha) }

private fun roundStroke(width: Float) = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)

internal fun DrawScope.drawGroundShadow(cx: Float, rx: Float) =
    oval(PetColors.Shadow, cx, GROUND_Y + 4f, rx, 5f)

/** A shape with rounded corners: a polygon filled and outlined in the same colour. */
internal fun DrawScope.roundedPolygon(color: Color, round: Float, points: List<Offset>) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    drawPath(path, color)
    drawPath(path, color, style = roundStroke(round))
}

// ---------------------------------------------------------------- grave

private fun DrawScope.drawGrave(species: Species) {
    val palette = species.palette()
    val stone = Color(0xFFC3C6D4)
    val stoneDark = Color(0xFF9296AB)
    drawGroundShadow(100f, 52f)
    val slab = Path().apply {
        moveTo(68f, 168f)
        lineTo(68f, 104f)
        cubicTo(68f, 66f, 132f, 66f, 132f, 104f)
        lineTo(132f, 168f)
        close()
    }
    drawPath(slab, stone)
    drawPath(slab, stoneDark, style = roundStroke(3f))
    // A small cross cut into the stone.
    drawLine(stoneDark, Offset(100f, 92f), Offset(100f, 130f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(stoneDark, Offset(88f, 105f), Offset(112f, 105f), strokeWidth = 5f, cap = StrokeCap.Round)
    // Grass at the foot, and a flower in the pet's colour.
    val grass = Color(0xFF6FB27F)
    for (x in listOf(60f, 72f, 128f, 140f)) {
        drawLine(grass, Offset(x, 168f), Offset(x - 3f, 156f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        drawLine(grass, Offset(x, 168f), Offset(x + 4f, 157f), strokeWidth = 3.5f, cap = StrokeCap.Round)
    }
    drawLine(grass, Offset(146f, 168f), Offset(146f, 150f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawCircle(palette.body, 7f, Offset(146f, 146f))
    drawCircle(PetColors.White, 3f, Offset(146f, 146f))
}

// ---------------------------------------------------------------- egg

private fun DrawScope.drawEgg(species: Species, wave: Float) {
    val palette = species.palette()
    drawGroundShadow(100f, 40f)
    // Eggs rock side to side instead of breathing.
    rotate(wave * 2f - 1f, Offset(100f, GROUND_Y)) {
        val path = Path().apply {
            moveTo(100f, 80f)
            cubicTo(128f, 80f, 140f, 118f, 140f, 142f)
            cubicTo(140f, 160f, 122f, 170f, 100f, 170f)
            cubicTo(78f, 170f, 60f, 160f, 60f, 142f)
            cubicTo(60f, 118f, 72f, 80f, 100f, 80f)
            close()
        }
        drawPath(path, PetColors.EggShell)
        drawCircle(palette.body, 8f, Offset(82f, 128f))
        drawCircle(palette.dark, 6f, Offset(116f, 112f))
        drawCircle(palette.body, 9f, Offset(110f, 146f))
        drawCircle(palette.dark, 4.5f, Offset(84f, 152f))
        drawCircle(palette.body, 4f, Offset(124f, 136f))
        rotatedOval(PetColors.White, 84f, 102f, 6f, 10f, 22f, alpha = .7f)
    }
}

// ---------------------------------------------------------------- face

private fun DrawScope.drawFace(look: PetLook, g: PetGeometry) {
    drawBlush(g)
    drawEyes(look.mood, g)
    g.mouthY?.let { drawMouth(look.mood, g, it) }
}

private fun DrawScope.drawBlush(g: PetGeometry) {
    for (s in listOf(-1f, 1f)) {
        oval(PetColors.Blush, g.cx + s * g.eyeDx * 1.55f, g.eyeY + g.eyeR * 2.4f, g.eyeR * 1.5f, g.eyeR, alpha = .55f)
    }
}

private fun DrawScope.drawEyes(mood: ArtMood, g: PetGeometry) {
    val er = g.eyeR
    for (s in listOf(-1f, 1f)) {
        val x = g.cx + s * g.eyeDx
        val y = g.eyeY
        when (mood) {
            ArtMood.Happy, ArtMood.Sleep -> {
                // Happy eyes arch up like ^ ^; sleeping eyes curve down like u u.
                val lift = if (mood == ArtMood.Happy) -er * 1.3f else er * 1.1f
                val dy = if (mood == ArtMood.Happy) er * .35f else 0f
                val path = Path().apply {
                    moveTo(x - er * 1.1f, y + dy)
                    quadraticTo(x, y + lift, x + er * 1.1f, y + dy)
                }
                drawPath(path, PetColors.Ink, style = roundStroke(er * .5f + 1.5f))
            }
            else -> {
                val r = if (mood == ArtMood.Hungry) er * 1.3f else er
                drawCircle(PetColors.Ink, r, Offset(x, y))
                drawCircle(PetColors.White, r * .34f, Offset(x + r * .3f, y - r * .32f))
                if (mood == ArtMood.Sad) {
                    drawLine(
                        PetColors.Ink,
                        Offset(x + s * er * 1.4f, y - er * 1.5f),
                        Offset(x - s * er * 1.2f, y - er * 2.3f),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round,
                    )
                    if (s < 0) oval(PetColors.Tear, x - er * .6f, y + er * 2.4f, er * .5f, er * .8f)
                }
            }
        }
    }
}

private fun DrawScope.drawMouth(mood: ArtMood, g: PetGeometry, y: Float) {
    val cx = g.cx
    val k = g.k
    val stroke = roundStroke(2.6f)
    when (mood) {
        ArtMood.Happy -> {
            val path = Path().apply {
                moveTo(cx - 8 * k, y)
                quadraticTo(cx, y + 15 * k, cx + 8 * k, y)
                close()
            }
            drawPath(path, PetColors.Ink)
            oval(PetColors.Tongue, cx, y + 7 * k, 4 * k, 2.6f * k)
        }
        ArtMood.Hungry -> oval(PetColors.Ink, cx, y + 3 * k, 5 * k, 7 * k)
        ArtMood.Sad -> {
            val path = Path().apply {
                moveTo(cx - 6 * k, y + 5 * k)
                quadraticTo(cx, y - 3 * k, cx + 6 * k, y + 5 * k)
            }
            drawPath(path, PetColors.Ink, style = stroke)
        }
        else -> {
            // A small "w", narrower while asleep.
            val w = (if (mood == ArtMood.Sleep) 5f else 7f) * k
            val path = Path().apply {
                moveTo(cx - w, y)
                quadraticTo(cx - w / 2, y + 6 * k, cx, y)
                quadraticTo(cx + w / 2, y + 6 * k, cx + w, y)
            }
            drawPath(path, PetColors.Ink, style = stroke)
        }
    }
}

/** Two "z"s that drift up and fade, one half a cycle behind the other. */
private fun DrawScope.drawSleepZs(g: PetGeometry, phase: Float) {
    fun z(x: Float, y: Float, size: Float, p: Float) {
        val a = if (p < .4f) p / .4f else (1f - p) / .6f
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + size, y)
            lineTo(x, y + size)
            lineTo(x + size, y + size)
        }
        translate(p * 6f, -p * 16f) {
            drawPath(path, Color(0xFF6B7FD6).copy(alpha = a.coerceIn(0f, 1f)), style = roundStroke(3f))
        }
    }
    z(g.cx + g.w0 * .8f, g.top + 6f, 15f, phase)
    z(g.cx + g.w0 * 1.05f, g.top - 14f, 10f, (phase + .5f) % 1f)
}
