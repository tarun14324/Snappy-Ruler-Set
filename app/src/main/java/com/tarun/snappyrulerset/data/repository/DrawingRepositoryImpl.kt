package com.tarun.snappyrulerset.data.repository

import com.tarun.snappyrulerset.domain.model.Shape
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import javax.inject.Inject

class DrawingRepositoryImpl @Inject constructor() : DrawingRepository {

    var drawing: List<Shape> = emptyList()

    override suspend fun load(): List<Shape> {
        return drawing
    }

    override suspend fun save(shapes: List<Shape>) {
        drawing = shapes
    }
}