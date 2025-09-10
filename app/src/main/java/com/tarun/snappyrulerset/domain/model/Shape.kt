package com.tarun.snappyrulerset.domain.model

sealed interface Shape { val id: String }

data class Polyline(
    override val id: String,
    val points: List<Point>,
) : Shape


data class CircleShape(
    override val id: String,
    val circle: Circle,
) : Shape
