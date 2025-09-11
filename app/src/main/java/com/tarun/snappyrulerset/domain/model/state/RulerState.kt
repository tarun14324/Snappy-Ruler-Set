package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.Point

data class RulerState(
    val position: Point,
    val length: Float = 400f,
    val angle: Float = 0f
)