package com.tarun.snappyrulerset.domain.repository

import android.net.Uri
import com.tarun.snappyrulerset.domain.model.Polyline

interface DrawingRepository {
    suspend fun saveDrawing(shapes: List<Polyline>)
    suspend fun loadDrawing(): List<Polyline>
    suspend fun exportDrawing(polylines: List<Polyline>): Uri?
}