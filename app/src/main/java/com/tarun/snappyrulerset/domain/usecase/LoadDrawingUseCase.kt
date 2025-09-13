package com.tarun.snappyrulerset.domain.usecase

import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import javax.inject.Inject

class LoadDrawingUseCase @Inject constructor(private val repo: DrawingRepository) {
    suspend operator fun invoke(): List<Polyline> = repo.loadDrawing()
}