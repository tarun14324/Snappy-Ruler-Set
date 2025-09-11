package com.tarun.snappyrulerset.utils.extensions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
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

fun DrawScope.drawSetSquare(sq: SetSquareState) {
    rotate(sq.angle, Offset(sq.position.x, sq.position.y)) {
        val size = sq.size
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(sq.position.x, sq.position.y)
            lineTo(sq.position.x + size, sq.position.y)
            lineTo(sq.position.x + if (sq.variant45) size else size * 0.5f, sq.position.y - size)
            close()
        }
        drawPath(path, Color.LightGray)
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