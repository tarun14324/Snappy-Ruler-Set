package com.tarun.snappyrulerset.domain.model

data class DrawingUiState(
    val shapes: List<Shape> = emptyList(),
    val currentPolyline: List<Point> = emptyList(),
    val snapCandidates: List<Point> = emptyList(),
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val hudText: String = "",
)