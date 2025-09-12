/**
 * Snapping engine: grid, common angles, nearby points
 * Priority: points > angle > grid (tweakable)
 */

package com.tarun.snappyrulerset.utils

import android.content.Context
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.SnapResult
import com.tarun.snappyrulerset.domain.model.SnapType
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlin.math.*

object SnapEngine {

    private val commonAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 150f, 180f)

    fun gridSizePx(context: Context, mm: Float = 5f): Float {
        val dpi = context.resources.displayMetrics.densityDpi.toFloat().coerceAtLeast(160f)
        return mm * (dpi / 25.4f)
    }

    /**
     * Combined snapping. Priority: point > angle > grid.
     * Uses vm.currentSnapRadiusPx() (dynamic with zoom).
     */
    fun snapPointToAll(
        input: Point,
        state: DrawingUiState,
        context: Context,
        vm: DrawingViewModel
    ): SnapResult {
        var snapType = SnapType.NONE
        var p = input
        val radius = try { vm.currentSnapRadiusPx() } catch (_: Throwable) { 32f }

        // 1) Snap to nearby points
        val pointSnap = snapToNearbyPoints(p, state, radius)
        if (pointSnap != p) {
            p = pointSnap
            snapType = SnapType.POINT
        }

        // 2) Snap to common angles
        val base = state.currentPolyline.lastOrNull() ?: p
        val angleSnap = snapToCommonAngles(base, p, threshold = 7f)
        if (angleSnap != p) {
            p = angleSnap
            snapType = SnapType.ANGLE
        }

        // 3) Snap to grid
        val gridSnap = snapToGrid(p, context)
        if (gridSnap != p) {
            p = gridSnap
            snapType = SnapType.GRID
        }

        return SnapResult(p, snapType)
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

    fun buildHudText(state: DrawingUiState, context: Context): String {
        val cur = state.currentPolyline
        if (cur.size >= 2) {
            val a = cur[cur.size - 2]
            val b = cur[cur.size - 1]
            val distCm = pxToCm(context, hypot((a.x - b.x), (a.y - b.y)))
            val absAngle = atan2(b.y - a.y, b.x - a.x).toDegrees()
            val normAngle = normalize(absAngle)
            return "Len: %.1f cm • Angle: %.1f°".format(distCm, normAngle)
        }

        // If protractor active and shapes exist, we can show measured angle between two rays if the protractor is placed near a vertex.
        state.protractorState?.let { pState ->
            // Try to find a vertex close to the protractor center and measure angle between two incident segments
            val measures = measureAngleAtNearestVertex(pState.position, state, maxDistancePx = 50f)
            if (measures != null) {
                val (angleDeg, _) = measures
                return "Protractor: %.1f°".format(angleDeg)
            }
        }

        return state.hudText ?: ""
    }

    /**
     * Try to find a vertex (point equality) near 'pos' and compute interior angle (deg) between two adjacent segments.
     * Returns Pair(angleDeg, vertexPoint) or null.
     */
    private fun measureAngleAtNearestVertex(pos: Point, state: DrawingUiState, maxDistancePx: Float): Pair<Float, Point>? {
        // scan all polylines for a vertex near pos
        state.shapes.forEach { poly ->
            val pts = poly.points
            for (i in 1 until pts.size - 1) { // vertex at pts[i] with neighbors pts[i-1], pts[i+1]
                val v = pts[i]
                val d = hypot(pos.x - v.x, pos.y - v.y)
                if (d <= maxDistancePx) {
                    val a = pts[i - 1]
                    val b = v
                    val c = pts[i + 1]
                    // compute angle at b between ba and bc
                    val bax = a.x - b.x; val bay = a.y - b.y
                    val bcx = c.x - b.x; val bcy = c.y - b.y
                    val dot = (bax * bcx + bay * bcy)
                    val mag = hypot(bax, bay) * hypot(bcx, bcy)
                    if (mag <= 0f) return null
                    val cosTheta = (dot / mag).coerceIn(-1.0f, 1.0f)
                    val angleDeg = Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
                    return Pair(angleDeg, v)
                }
            }
        }
        return null
    }

    // --- Helpers ---
    private fun pxToCm(context: Context, px: Float): Float {
        val dpi = context.resources.displayMetrics.densityDpi.toFloat().coerceAtLeast(160f)
        val pxPerCm = dpi / 2.54f
        val cm = px / pxPerCm
        return (kotlin.math.round(cm * 10f) / 10f)
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
}

