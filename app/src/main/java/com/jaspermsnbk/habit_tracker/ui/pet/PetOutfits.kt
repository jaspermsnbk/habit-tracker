package com.jaspermsnbk.habit_tracker.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Parts of an outfit that sit behind the pet: a calm aura, or speed lines. */
internal fun DrawScope.drawOutfitBack(outfit: Outfit, g: PetGeometry) {
    when (outfit) {
        Outfit.Mindful -> {
            drawCircle(PetColors.Aura.copy(alpha = .14f), g.w0 * 1.5f, Offset(g.cx, g.cy))
            drawCircle(PetColors.Aura.copy(alpha = .12f), g.w0 * 1.25f, Offset(g.cx, g.cy))
        }
        Outfit.Runner -> {
            val x0 = g.cx - g.w0 - 8f
            for ((dy, length) in listOf(-.3f to 30f, 0f to 40f, .3f to 24f)) {
                val y = g.cy + dy * g.h0
                drawLine(PetColors.Ink.copy(alpha = .28f), Offset(x0, y), Offset(x0 - length, y), 4f, StrokeCap.Round)
            }
        }
        else -> Unit
    }
}

/** Parts of an outfit that sit in front: headband, glasses, a leaf. */
internal fun DrawScope.drawOutfitFront(outfit: Outfit, g: PetGeometry) {
    when (outfit) {
        Outfit.Runner -> drawRunnerGear(g)
        Outfit.Reader -> drawReaderGear(g)
        Outfit.Mindful -> drawMindfulGear(g)
        Outfit.None -> Unit
    }
}

private fun DrawScope.drawRunnerGear(g: PetGeometry) {
    val k = g.k
    val y0 = g.top + .42f * g.h0
    val half = .82f * g.w0
    val band = Path().apply {
        moveTo(g.cx - half, y0)
        quadraticTo(g.cx, y0 + .12f * g.h0, g.cx + half, y0)
    }
    drawPath(band, PetColors.Red, style = Stroke(8f * k, cap = StrokeCap.Round))
    val knot = Path().apply {
        moveTo(g.cx + half - 2f, y0)
        relativeLineTo(13f * k, 9f * k)
        relativeMoveTo(-13f * k, -9f * k)
        relativeLineTo(15f * k, -4f * k)
    }
    drawPath(knot, PetColors.Red, style = Stroke(4.5f * k, cap = StrokeCap.Round))
    for (s in listOf(-1f, 1f)) {
        val x = g.cx + s * g.footDx
        oval(PetColors.Red, x, 165.5f, 15f * k, 7.5f * k)
        drawLine(PetColors.White, Offset(x - 14f * k, 169.5f), Offset(x + 14f * k, 169.5f), 3f * k, StrokeCap.Round)
    }
}

private fun DrawScope.drawReaderGear(g: PetGeometry) {
    val k = g.k
    val lens = g.eyeR * 2.3f
    for (s in listOf(-1f, 1f)) {
        val c = Offset(g.cx + s * g.eyeDx, g.eyeY)
        drawCircle(PetColors.White.copy(alpha = .28f), lens, c)
        drawCircle(PetColors.Ink, lens, c, style = Stroke(2.4f))
    }
    drawLine(PetColors.Ink, Offset(g.cx - g.eyeDx + lens, g.eyeY), Offset(g.cx + g.eyeDx - lens, g.eyeY), 2.4f)

    // An open book held in front of the belly.
    val by = g.cy + g.h0 - 24f * k
    val bw = 22f * k
    for (s in listOf(-1f, 1f)) {
        val page = Path().apply {
            moveTo(g.cx, by + 3f * k)
            lineTo(g.cx + s * bw, by)
            lineTo(g.cx + s * bw, by + 22f * k)
            lineTo(g.cx, by + 25f * k)
            close()
        }
        drawPath(page, if (s < 0) PetColors.White else PetColors.BookPage)
        drawPath(page, PetColors.Book, style = Stroke(2.2f, join = StrokeJoin.Round))
        for (row in listOf(8f, 14f)) {
            drawLine(
                PetColors.Book.copy(alpha = .7f),
                Offset(g.cx + s * 4f * k, by + (row + 2f) * k),
                Offset(g.cx + s * bw * .7f, by + (row + 0f) * k),
                1.6f,
                StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawMindfulGear(g: PetGeometry) {
    val k = g.k
    val lx = g.cx + .12f * g.w0
    val ly = g.top - 2f * k
    drawLine(PetColors.LeafStem, Offset(lx, ly + 4f * k), Offset(lx - k, ly - 8f * k), 2.4f, StrokeCap.Round)
    rotatedOval(PetColors.Leaf, lx + 7f * k, ly - 14f * k, 7f * k, 14f * k, 35f)
    for ((dx, dy, size) in listOf(
        Triple(-g.w0 - 14f, -.5f, 7f),
        Triple(g.w0 + 12f, -.75f, 9f),
        Triple(g.w0 + 4f, .2f, 5f),
    )) {
        drawSparkle(g.cx + dx, g.cy + dy * g.h0, size * k)
    }
}

/** A four-pointed star. */
private fun DrawScope.drawSparkle(x: Float, y: Float, r: Float) {
    val star = Path().apply {
        moveTo(x, y - r)
        quadraticTo(x, y, x + r, y)
        quadraticTo(x, y, x, y + r)
        quadraticTo(x, y, x - r, y)
        quadraticTo(x, y, x, y - r)
        close()
    }
    drawPath(star, PetColors.Sparkle)
}
