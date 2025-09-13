package com.tarun.snappyrulerset.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class Point(
    val x: Float = 0f,
    val y: Float = 0f
)