package com.tarun.snappyrulerset.utils.extensions

import androidx.compose.ui.geometry.Offset
import com.tarun.snappyrulerset.domain.model.Point
import kotlin.math.cos
import kotlin.math.sin


// Extension to add a Point and an Offset
operator fun Point.plus(offset: Offset): Point =
    Point(this.x + offset.x, this.y + offset.y)

// Extension to add two Points
operator fun Point.plus(other: Point): Point =
    Point(this.x + other.x, this.y + other.y)

// Extension to subtract two Points
operator fun Point.minus(other: Point): Point =
    Point(this.x - other.x, this.y - other.y)

// Convert Point to Offset (useful for Canvas drawing)
fun Point.toOffset(): Offset = Offset(x, y)

// Convert Offset to Point (useful for pointer input events)
fun Offset.toPoint(): Point = Point(x, y)
fun Float.toDegrees(): Float = (this * 180f / Math.PI).toFloat()
fun Float.format(digits: Int) = "%.${digits}f".format(this)
fun Float.toRadians(): Float = (this / 180f) * Math.PI.toFloat()

fun pxToCm(px: Float): Float = px / 160f / 2.54f


fun projectPointOntoLine(p: Point, lineStart: Point, angleDeg: Float): Point {
    val rad = angleDeg.toRadians()
    val dx = cos(rad)
    val dy = sin(rad)
    val t = ((p.x - lineStart.x) * dx + (p.y - lineStart.y) * dy)
    return Point(lineStart.x + t * dx, lineStart.y + t * dy)
}
