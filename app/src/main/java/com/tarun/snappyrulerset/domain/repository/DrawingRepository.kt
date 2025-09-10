package com.tarun.snappyrulerset.domain.repository

import com.tarun.snappyrulerset.domain.model.Shape

interface DrawingRepository {
    suspend fun save(shapes: List<Shape>)
    suspend fun load(): List<Shape>
}