package com.tarun.snappyrulerset.domain.model.state

import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline

data class DrawingUiState(
    var activeTool: ActiveTool = ActiveTool.PEN,
    val shapes: List<Polyline> = emptyList(),
    var currentPolyline: List<Point> = emptyList(),
    val rulerState: RulerState? = null,
    val setSquareState: SetSquareState? = null,
    val protractorState: ProtractorState? = null,
    val compassState: CompassState? = null,
    val hudText: String? = "",
    var zoomLevel: Float = 1f
)