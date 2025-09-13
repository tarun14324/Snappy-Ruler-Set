package com.tarun.snappyrulerset.domain.usecase

import android.net.Uri
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import javax.inject.Inject

class ExportDrawingUseCase @Inject constructor(
    private val repository: DrawingRepository,
) {
    suspend operator fun invoke(polylines: List<Polyline>): Uri? {
        return repository.exportDrawing(polylines)
    }
}
