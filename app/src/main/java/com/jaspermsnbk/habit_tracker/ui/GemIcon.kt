package com.jaspermsnbk.habit_tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GemLight = Color(0xFF8DE8F2)
private val GemMid = Color(0xFF35B8D8)
private val GemDark = Color(0xFF1E88A8)

/** A cut gem, drawn in a few facets so it needs no image file. */
@Composable
fun GemIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun pt(x: Float, y: Float) = Offset(x * w, y * h)
        fun facet(color: Color, points: List<Offset>) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                close()
            }
            drawPath(path, color)
        }
        facet(GemLight, listOf(pt(.2f, .15f), pt(.5f, .15f), pt(.5f, .4f), pt(0f, .4f)))
        facet(GemMid, listOf(pt(.5f, .15f), pt(.8f, .15f), pt(1f, .4f), pt(.5f, .4f)))
        facet(GemMid, listOf(pt(0f, .4f), pt(.5f, .4f), pt(.5f, 1f)))
        facet(GemDark, listOf(pt(.5f, .4f), pt(1f, .4f), pt(.5f, 1f)))
    }
}

/** A gem and a count, like "30". Read out as "30 gems". */
@Composable
fun GemBalance(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "$count ${if (count == 1) "gem" else "gems"}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GemIcon()
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
