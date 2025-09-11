package com.tarun.snappyrulerset.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.FileProvider
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.model.SerializablePolyline
import com.tarun.snappyrulerset.domain.model.Shape
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import kotlinx.serialization.json.Json
import java.io.File
import androidx.core.graphics.createBitmap
import java.io.FileOutputStream
import javax.inject.Inject

class DrawingRepositoryImpl @Inject constructor(val context: Context) : DrawingRepository {

    private val file: File = File(context.filesDir, "drawing.json")
    private val json = Json { prettyPrint = true }

    override suspend fun saveDrawing(shapes: List<Shape>) {
        val serializable = shapes.filterIsInstance<Polyline>().map {
            SerializablePolyline(it.id, it.points)
        }
        file.writeText(json.encodeToString(serializable)).apply { }
    }

    override suspend fun loadDrawing(): List<Shape> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        val decoded = json.decodeFromString<List<SerializablePolyline>>(text)
        return decoded.map { Polyline(it.id, it.points) }
    }

    override suspend fun exportDrawing(polylines: List<Polyline>): Uri? {
        val width = 1080
        val height = 1920
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.CYAN
            strokeWidth = 4f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }

        polylines.forEach { poly ->
            poly.points.windowed(2).forEach { (a, b) ->
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            }
        }

        val file = File(context.cacheDir, "snappy_export_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}