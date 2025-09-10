package com.tarun.snappyrulerset.domain.snap

import com.tarun.snappyrulerset.domain.model.Line
import com.tarun.snappyrulerset.domain.model.Point
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round

class SnapEngine {
    data class Config(
        val gridSpacingPx: Float = 20f, // ~5 mm at 160dpi
        val baseSnapRadiusPx: Float = 24f,
    )


    data class Result(val snapped: Point, val reason: Reason) {
        enum class Reason { NONE, GRID, POINT, ANGLE }
    }

    fun snap(
        raw: Point,
        zoom: Float,
        points: List<Point>,
        angleLine: Line?,
        config: Config = Config(),
    ): Result {
        val radius = (config.baseSnapRadiusPx / zoom).coerceAtLeast(6f)
        var best: Pair<Point, Result.Reason>? = null


// 1) Points
        points.forEach { p ->
            if (raw.distanceTo(p) <= radius) {
                best = p to Result.Reason.POINT
            }
        }


// 2) Grid
        val gx = round(raw.x / config.gridSpacingPx) * config.gridSpacingPx
        val gy = round(raw.y / config.gridSpacingPx) * config.gridSpacingPx
        val gpt = Point(gx, gy)
        if (raw.distanceTo(gpt) < (best?.first?.let { raw.distanceTo(it) } ?: Float.MAX_VALUE)) {
            if (raw.distanceTo(gpt) <= radius) best = gpt to Result.Reason.GRID
        }


// 3) Angle snapping relative to a line (e.g., ruler/set square)
        if (angleLine != null) {
            val angle = angleDeg(angleLine)
            val snappedAng = snapAngle(angle)
            val snapped = rotateAround(angleLine.start, raw, angle - snappedAng)
            if (raw.distanceTo(snapped) <= radius) {
                if (best == null || raw.distanceTo(snapped) < raw.distanceTo(best.first)) {
                    best = snapped to Result.Reason.ANGLE
                }
            }
        }


        return if (best != null) Result(best.first, best.second) else Result(
            raw,
            Result.Reason.NONE
        )
    }


    private fun angleDeg(l: Line): Double =
        Math.toDegrees(atan2((l.end.y - l.start.y), (l.end.x - l.start.x)).toDouble())


    private fun snapAngle(deg: Double): Double {
        val common = listOf(0.0, 30.0, 45.0, 60.0, 90.0, 120.0, 135.0, 150.0, 180.0)
// Find nearest common within ~7 deg, else round to nearest degree
        var best = deg
        var min = Double.MAX_VALUE
        for (c in common) {
            val d = abs(normDeg(deg - c))
            if (d < min && d <= 7.0) {
                min = d; best = c
            }
        }
        if (min == Double.MAX_VALUE) {
            best = (deg).let { round(it) }
        }
        return best
    }


    private fun normDeg(d: Double): Double {
        var a = d % 360.0
        if (a < -180) a += 360.0
        if (a > 180) a -= 360.0
        return a
    }


    private fun rotateAround(origin: Point, p: Point, deltaDeg: Double): Point {
        val rad = deltaDeg * PI / 180.0
        val s = kotlin.math.sin(rad)
        val c = kotlin.math.cos(rad)
        val tx = p.x - origin.x
        val ty = p.y - origin.y
        val rx = tx * c - ty * s
        val ry = tx * s + ty * c
        return Point((rx + origin.x).toFloat(), (ry + origin.y).toFloat())
    }
}