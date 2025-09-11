package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.Point

data class ProtractorState(
    val position: Point,
    val radius: Float = 120f,
    val angle: Float
)