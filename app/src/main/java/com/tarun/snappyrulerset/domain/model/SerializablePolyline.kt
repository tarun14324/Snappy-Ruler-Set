package com.tarun.snappyrulerset.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePolyline(
    val id: String,
    val points: List<Point>
)