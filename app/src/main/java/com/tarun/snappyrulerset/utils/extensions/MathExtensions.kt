package com.tarun.snappyrulerset.utils.extensions

import androidx.compose.ui.geometry.Offset
import com.tarun.snappyrulerset.domain.model.Point

// --- Extensions ---
fun Offset.toPoint() = Point(x, y)
fun Point.toOffset() = Offset(x, y)
fun Float.toDegrees(): Float = (this * 180f / Math.PI).toFloat()
fun Float.format(digits: Int) = "%.${digits}f".format(this)