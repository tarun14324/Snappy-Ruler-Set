package com.tarun.snappyrulerset.utils.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.tarun.snappyrulerset.domain.model.SetSquareVariant
import com.tarun.snappyrulerset.domain.model.state.CompassState
import com.tarun.snappyrulerset.domain.model.state.ProtractorState
import com.tarun.snappyrulerset.domain.model.state.RulerState
import com.tarun.snappyrulerset.domain.model.state.SetSquareState
import kotlin.math.cos
import kotlin.math.sin


// --- Tool drawing helpers ---
fun DrawScope.drawRuler(ruler: RulerState) {
    rotate(ruler.angle, Offset(ruler.position.x, ruler.position.y)) {
        drawRect(
            Color.Gray,
            topLeft = Offset(ruler.position.x - ruler.length / 2, ruler.position.y - 10f),
            size = Size(ruler.length, 20f)
        )
    }
}

fun DrawScope.drawSetSquare(state: SetSquareState) {
    val center = state.position.toOffset()
    val size = state.size

    // Define triangle points depending on variant
    val points: List<Offset> = when (state.variant) {
        SetSquareVariant.DEG_45 -> {
            listOf(
                Offset(center.x, center.y - size),             // top
                Offset(center.x - size, center.y + size),      // bottom left
                Offset(center.x + size, center.y + size)       // bottom right
            )
        }
        SetSquareVariant.DEG_30_60 -> {
            listOf(
                Offset(center.x, center.y - size),                       // top
                Offset(center.x - (size * 0.866f), center.y + size),     // bottom left (sin60=0.866)
                Offset(center.x + (size * 0.5f), center.y + size)        // bottom right (cos60=0.5)
            )
        }
    }

    // Convert angle to radians
    val rad = state.angle.toRadians()
    val rotatedPoints = points.map {
        val dx = it.x - center.x
        val dy = it.y - center.y
        val xRot = dx * cos(rad) - dy * sin(rad)
        val yRot = dx * sin(rad) + dy * cos(rad)
        Offset(center.x + xRot, center.y + yRot)
    }

    // Draw triangle edges in a cycle
    for (i in rotatedPoints.indices) {
        val a = rotatedPoints[i]
        val b = rotatedPoints[(i + 1) % rotatedPoints.size]
        drawLine(Color(0xFFFD971F), a, b, strokeWidth = 4f)
    }
}

fun DrawScope.drawProtractorWithTicks(p: ProtractorState) {
    rotate(p.angle, Offset(p.position.x, p.position.y)) {
        drawArc(
            Color(0x80FFFFFF),
            startAngle = -180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(p.position.x - p.radius, p.position.y - p.radius),
            size = Size(p.radius * 2, p.radius)
        )
        val center = Offset(p.position.x, p.position.y)
        val inner = p.radius - 6f
        for (deg in -180..0 step 5) {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val cos = cos(rad);
            val sin = sin(rad)
            val start = center + Offset(cos * inner, sin * inner)
            val len = if (deg % 30 == 0) 12f else if (deg % 10 == 0) 8f else 4f
            val end = center + Offset(cos * (inner + len), sin * (inner + len))
            drawLine(Color.White, start, end, 1.5f)
        }
    }
}

fun DrawScope.drawCompass(c: CompassState) {
    val center = Offset(c.center.x, c.center.y)
    drawCircle(Color(0x55FFFFFF), radius = c.radius, center = center)
    drawCircle(Color(0xFFFFD86E), radius = 4f, center = center)
}