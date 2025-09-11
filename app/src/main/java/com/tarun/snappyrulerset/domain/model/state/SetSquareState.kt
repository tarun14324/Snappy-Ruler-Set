package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.Point

data class SetSquareState(
    val position: Point,
    val angle: Float,
    val variant45: Boolean = true,
    val size: Float
)