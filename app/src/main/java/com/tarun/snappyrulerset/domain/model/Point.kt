package com.tarun.snappyrulerset.domain.model

import kotlin.math.hypot

data class Point(val x: Float, val y: Float) {
    fun distanceTo(other: Point): Float = hypot((x - other.x), (y - other.y))
}