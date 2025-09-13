
package com.tarun.snappyrulerset.utils

import android.content.Context
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.SnapResult
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlin.math.*

object SnapEngine {

    private val commonAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f)

    fun gridSizePx(context: Context, mm: Float = 5f): Float {
        val dpi = context.resources.displayMetrics.densityDpi.toFloat().coerceAtLeast(160f)
        return mm * (dpi / 25.4f)
    }

    data class SnapCandidates(
        val chosen: SnapResult,
        val alternatives: List<SnapResult>
    )

    fun snapPointToAll(
        input: Point,
        state: DrawingUiState,
        context: Context,
        vm: DrawingViewModel
    ): SnapCandidates {
        val results = mutableListOf<SnapResult>()

        // 1) Snap to nearby points
        val radius = try { vm.currentSnapRadiusPx() } catch (_: Throwable) { 32f }
        val snappedToPoint = snapToNearbyPoints(input, state, radius)
        if (snappedToPoint != input) results += SnapResult(snappedToPoint, "Point")

        // 2) Snap to common angles
        val base = state.currentPolyline.lastOrNull() ?: input
        val snappedToAngle = snapToCommonAngles(base, input, threshold = 7f)
        if (snappedToAngle != input) results += SnapResult(snappedToAngle, "Angle")

        // 3) Snap to grid
        val snappedToGrid = snapToGrid(input, context)
        if (snappedToGrid != input) results += SnapResult(snappedToGrid, "Grid")

        // Choose closest
        val chosen = results.minByOrNull { hypot(it.point.x - input.x, it.point.y - input.y) }
            ?: SnapResult(input, "None")

        return SnapCandidates(chosen, results.filter { it != chosen })
    }

    fun snapToGrid(p: Point, context: Context, mm: Float = 5f): Point {
        val step = gridSizePx(context, mm)
        if (step <= 0f) return p
        val x = (p.x / step).roundToInt() * step
        val y = (p.y / step).roundToInt() * step
        return Point(x, y)
    }

    fun snapToCommonAngles(origin: Point, target: Point, threshold: Float = 6f): Point {
        val dx = target.x - origin.x
        val dy = target.y - origin.y
        if (dx == 0f && dy == 0f) return target
        val angle = atan2(dy, dx).toDegrees()
        val normAngle = normalize(angle)
        val nearest = commonAngles.minByOrNull { abs(diffAngle(normAngle, it)) } ?: normAngle
        val diff = abs(diffAngle(normAngle, nearest))
        return if (diff <= threshold) {
            val dist = hypot(dx, dy)
            val rad = nearest.toRadians()
            Point(origin.x + cos(rad) * dist, origin.y + sin(rad) * dist)
        } else target
    }

    fun snapToNearbyPoints(p: Point, state: DrawingUiState, radius: Float): Point {
        val candidates = mutableListOf<Point>()
        state.shapes.forEach { poly ->
            candidates += poly.points
            poly.points.windowed(2).forEach { (a, b) ->
                candidates += Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
            }
        }
        state.shapes.forEach { poly ->
            poly.points.windowed(2).forEach { (a1, a2) ->
                state.shapes.forEach { other ->
                    other.points.windowed(2).forEach { (b1, b2) ->
                        val intersect = lineIntersection(a1, a2, b1, b2)
                        if (intersect != null) candidates += intersect
                    }
                }
            }
        }

        state.rulerState?.let { candidates += it.position }
        state.setSquareState?.let { candidates += it.position }
        state.protractorState?.let { candidates += it.position }
        state.compassState?.let { candidates += it.center }

        var best: Point? = null
        var bestDist = Float.MAX_VALUE
        candidates.forEach { c ->
            val d = hypot(p.x - c.x, p.y - c.y)
            if (d < bestDist && d <= radius) {
                bestDist = d
                best = c
            }
        }
        return best ?: p
    }

    private fun Float.toRadians() = (this / 180f) * Math.PI.toFloat()
    private fun Float.toDegrees() = (this * 180f / Math.PI).toFloat()
    private fun normalize(a: Float): Float {
        var x = a % 360f
        if (x < 0f) x += 360f
        return x
    }
    private fun diffAngle(a: Float, b: Float): Float {
        val d = (a - b + 180f) % 360f - 180f
        return d
    }

    private fun lineIntersection(p1: Point, p2: Point, p3: Point, p4: Point): Point? {
        val denom = (p1.x - p2.x) * (p3.y - p4.y) - (p1.y - p2.y) * (p3.x - p4.x)
        if (denom == 0f) return null
        val x = ((p1.x*p2.y - p1.y*p2.x)*(p3.x - p4.x) - (p1.x - p2.x)*(p3.x*p4.y - p3.y*p4.x)) / denom
        val y = ((p1.x*p2.y - p1.y*p2.x)*(p3.y - p4.y) - (p1.y - p2.y)*(p3.x*p4.y - p3.y*p4.x)) / denom
        return Point(x, y)
    }
}

