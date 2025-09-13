package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.SetSquareVariant

data class SetSquareState(
    val position: Point,
    val angle: Float,
    val variant45: Boolean = true,
    val size: Float,
    val variant: SetSquareVariant = SetSquareVariant.DEG_45
)