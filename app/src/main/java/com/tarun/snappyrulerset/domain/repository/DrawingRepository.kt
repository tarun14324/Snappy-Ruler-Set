package com.tarun.snappyrulerset.domain.repository

import android.net.Uri
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.model.Shape

interface DrawingRepository {
    suspend fun saveDrawing(shapes: List<Shape>)
    suspend fun loadDrawing(): List<Shape>
    suspend fun exportDrawing(polylines: List<Polyline>): Uri?
}