package com.tarun.snappyrulerset.domain.model

import kotlinx.serialization.Serializable

sealed interface Shape { val id: String }

@Serializable
data class Polyline(
    override val id: String,
    val points: List<Point>,
) : Shape

