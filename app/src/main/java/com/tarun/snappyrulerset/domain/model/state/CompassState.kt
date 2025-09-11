package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.Point

data class CompassState(
    val center: Point,
    val radius: Float
)