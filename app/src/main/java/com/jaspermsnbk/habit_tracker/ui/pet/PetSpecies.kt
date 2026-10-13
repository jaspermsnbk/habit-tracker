package com.jaspermsnbk.habit_tracker.ui.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Each function draws one species' body, without the face or outfit. Stage 1 is the baby,
// 2 the teen and 3 the adult; details are added as the pet grows.

/** Arms swing back when the pet wears running gear, and grow a little. */
private fun armSpec(outfit: Outfit): Pair<Float, Float> =
    if (outfit == Outfit.Runner) 1.25f to -35f else 1f to 22f

private fun DrawScope.drawArms(look: PetLook, g: PetGeometry, color: Color, offsetX: Float, offsetY: Float) {
    if (look.stage < 2) return
    val (size, angle) = armSpec(look.outfit)
    for (s in listOf(-1f, 1f)) {
        val ax = g.cx + s * offsetX * g.w0
        val ay = g.cy + offsetY * g.h0
        rotatedOval(color, ax, ay, 7.5f * g.k * size, 12f * g.k * size, s * angle)
    }
}

private fun DrawScope.drawHighlight(g: PetGeometry, x: Float = -.45f, y: Float = .28f) {
    val hx = g.cx + x * g.w0
    val hy = g.top + y * g.h0
    rotatedOval(PetColors.White, hx, hy, .13f * g.w0, .07f * g.h0 + 1.5f, -30f, alpha = .4f)
}

internal fun DrawScope.drawMochi(look: PetLook, g: PetGeometry, p: Palette) {
    val rx = g.w0
    val ry = g.h0
    val cx = g.cx
    val cy = g.cy
    val top = g.top
    val k = g.k
    val earH = ry * .55f
    if (look.stage >= 2) {
        val tail = Path().apply {
            moveTo(cx + .85f * rx, cy + .55f * ry)
            cubicTo(cx + 1.55f * rx, cy + .65f * ry, cx + 1.45f * rx, cy - .2f * ry, cx + 1.15f * rx, cy - .38f * ry)
        }
        drawPath(tail, p.body, style = Stroke(9f * k, cap = StrokeCap.Round))
        if (look.stage == 3) drawCircle(p.dark, 7f * k, Offset(cx + 1.15f * rx, cy - .38f * ry))
    }
    val innerEar = lerp(p.body, p.dark, .7f)
    for (s in listOf(-1f, 1f)) {
        roundedPolygon(
            p.body, 7f,
            listOf(
                Offset(cx + s * .8f * rx, top + .5f * ry),
                Offset(cx + s * .6f * rx, top - earH * .85f),
                Offset(cx + s * .15f * rx, top + .12f * ry),
            ),
        )
        roundedPolygon(
            innerEar, 3f,
            listOf(
                Offset(cx + s * .68f * rx, top + .3f * ry),
                Offset(cx + s * .6f * rx, top - earH * .35f),
                Offset(cx + s * .34f * rx, top + .14f * ry),
            ),
        )
    }
    oval(p.body, cx, cy, rx, ry)
    oval(p.belly, cx, cy + .42f * ry, .58f * rx, .5f * ry)
    drawHighlight(g)
    for (s in listOf(-1f, 1f)) oval(p.dark, cx + s * g.footDx, 165f, 12f * k, 6.5f * k)
    drawArms(look, g, p.body, .96f, .22f)
    if (look.stage == 3) {
        val mark = Path().apply {
            moveTo(cx, top + .18f * ry)
            relativeLineTo(5f * k, 7f * k)
            relativeLineTo(-5f * k, 7f * k)
            relativeLineTo(-5f * k, -7f * k)
            close()
        }
        drawPath(mark, PetColors.Aura)
        for (s in listOf(-1f, 1f)) {
            val tuft = Path().apply {
                moveTo(cx + s * (rx - 1f), cy + .1f * ry)
                relativeLineTo(s * 9f * k, 4f * k)
                relativeLineTo(-s * 3f * k, 5f * k)
                relativeLineTo(s * 7f * k, 6f * k)
                lineTo(cx + s * (rx - 6f), cy + .45f * ry)
                close()
            }
            drawPath(tuft, p.body)
        }
    }
}

internal fun DrawScope.drawPip(look: PetLook, g: PetGeometry, p: Palette) {
    val r = g.w0
    val cx = g.cx
    val cy = g.cy
    val top = g.top
    val k = g.k
    if (look.stage == 3) {
        rotatedOval(p.dark, cx + r * .95f, cy + r * .55f, 7f * k, 20f * k, -62f)
        rotatedOval(PetColors.Red, cx + r * .85f, cy + r * .7f, 6f * k, 18f * k, -80f)
    }
    for (s in listOf(-1f, 1f)) {
        val x = cx + s * g.footDx
        val y = cy + r - 1f
        val foot = Path().apply {
            moveTo(x, y)
            relativeLineTo(0f, 8f * k)
            relativeMoveTo(-5f * k, 0f)
            relativeLineTo(10f * k, 0f)
        }
        drawPath(foot, Color(0xFFE8832A), style = Stroke(3.2f * k, cap = StrokeCap.Round))
    }
    drawCircle(p.body, r, Offset(cx, cy))
    oval(p.belly, cx, cy + .38f * r, .6f * r, .5f * r)
    val tufts = intArrayOf(0, 2, 3, 5)[look.stage]
    val tuftColor = if (look.stage == 3) PetColors.Red else p.dark
    for (i in 0 until tufts) {
        val f = if (tufts == 1) 0f else i / (tufts - 1f) - .5f
        val ang = f * 70f * Math.PI.toFloat() / 180f
        val bx = cx + sin(ang) * r * .22f
        val by = top + r * .06f + abs(f) * r * .1f
        val reach = .4f + if (look.stage == 3) .12f else 0f
        val tx = cx + sin(ang) * r * reach
        val ty = top - r * (.34f - abs(f) * .12f)
        val feather = Path().apply {
            moveTo(bx, by + 4f)
            quadraticTo(bx + sin(ang) * 10f, ty + 8f, tx, ty)
        }
        drawPath(feather, tuftColor, style = Stroke(6.5f * k, cap = StrokeCap.Round))
    }
    val wingAngle = if (look.outfit == Outfit.Runner) -42f else 22f
    for (s in listOf(-1f, 1f)) {
        rotatedOval(p.dark, cx + s * r * .96f, cy + .12f * r, r * .2f, r * .34f, s * wingAngle)
    }
    drawHighlight(g, x = -.5f, y = .3f)
    // The beak is part of the body; it opens a little when the pet is hungry or happy.
    val open = look.mood == ArtMood.Hungry || look.mood == ArtMood.Happy
    val beakTip = cy + (if (open) .24f else .17f) * r
    val beak = Path().apply {
        moveTo(cx - .17f * r, cy - .04f * r)
        lineTo(cx + .17f * r, cy - .04f * r)
        lineTo(cx, beakTip)
        close()
    }
    drawPath(beak, Color(0xFFF08A24))
    drawPath(beak, Color(0xFFF08A24), style = Stroke(2.5f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    if (open) {
        val inner = Path().apply {
            moveTo(cx - .1f * r, cy + .07f * r)
            lineTo(cx + .1f * r, cy + .07f * r)
            lineTo(cx, cy + .2f * r)
            close()
        }
        drawPath(inner, Color(0xFFB5471C))
    }
}

internal fun DrawScope.drawBun(look: PetLook, g: PetGeometry, p: Palette) {
    val rx = g.w0
    val ry = g.h0
    val cx = g.cx
    val cy = g.cy
    val top = g.top
    val k = g.k
    if (look.stage >= 2) drawCircle(p.belly, 9f * k, Offset(cx + .8f * rx, cy + .7f * ry))
    // Ears grow taller than the head is wide, and lean outward.
    val earRx = .22f * rx
    val earRy = ry * (.5f + .06f * look.stage)
    val earCy = top + .05f * ry - earRy * .55f
    val innerEar = lerp(p.body, p.dark, .45f)
    for (s in listOf(-1f, 1f)) {
        val ex = cx + s * .4f * rx
        rotatedOval(p.body, ex, earCy, earRx, earRy, s * 10f)
        rotatedOval(innerEar, ex, earCy + earRy * .1f, earRx * .5f, earRy * .72f, s * 10f)
    }
    oval(p.body, cx, cy, rx, ry)
    oval(p.belly, cx, cy + .42f * ry, .56f * rx, .48f * ry)
    drawHighlight(g)
    for (s in listOf(-1f, 1f)) oval(p.dark, cx + s * g.footDx, 165f, 13f * k, 6f * k)
    drawArms(look, g, p.body, .96f, .24f)
    oval(p.dark, cx, cy + .13f * ry, 3.4f * k, 2.4f * k)
    if (look.stage == 3) {
        for (s in listOf(-1f, 1f)) {
            for (dy in listOf(-4f, 3f)) {
                drawLine(
                    PetColors.Ink.copy(alpha = .45f),
                    Offset(cx + s * .5f * rx, cy + .22f * ry + dy * k),
                    Offset(cx + s * (.5f * rx + 15f * k), cy + .22f * ry + dy * k * 2.2f),
                    strokeWidth = 1.6f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

internal fun DrawScope.drawShell(look: PetLook, g: PetGeometry, p: Palette) {
    val rx = g.w0
    val ry = g.h0
    val cx = g.cx
    val cy = g.cy
    val top = g.top
    val k = g.k
    val shellColor = lerp(p.dark, Color(0xFF2F7F5A), .5f)
    val plate = lerp(shellColor, Color.Black, .18f)
    // The shell is a dome behind the head, with a few plates showing above it.
    oval(shellColor, cx, cy - .25f * ry, rx * 1.08f, ry * 1.05f)
    oval(plate, cx, top - .08f * ry, .3f * rx, .18f * ry)
    rotatedOval(plate, cx - .62f * rx, top + .1f * ry, .2f * rx, .16f * ry, -28f)
    rotatedOval(plate, cx + .62f * rx, top + .1f * ry, .2f * rx, .16f * ry, 28f)
    if (look.stage == 3) {
        for (s in listOf(-1f, 1f)) {
            val x = cx + s * .12f * rx
            val y = top - .42f * ry
            rotatedOval(PetColors.Leaf, x + s * 6f * k, y, 5f * k, 11f * k, s * 35f)
        }
        drawLine(PetColors.LeafStem, Offset(cx, top - .28f * ry), Offset(cx, top - .5f * ry), 2.4f, StrokeCap.Round)
    }
    if (look.stage >= 2) {
        val tail = Path().apply {
            moveTo(cx + .9f * rx, cy + .6f * ry)
            relativeLineTo(10f * k, 2f * k)
            relativeLineTo(-8f * k, 7f * k)
            close()
        }
        drawPath(tail, p.dark)
    }
    oval(p.body, cx, cy, rx, ry)
    oval(p.belly, cx, cy + .42f * ry, .58f * rx, .48f * ry)
    drawHighlight(g)
    for (s in listOf(-1f, 1f)) oval(p.dark, cx + s * g.footDx, 165f, 13f * k, 6.5f * k)
    drawArms(look, g, p.body, 1f, .3f)
}
