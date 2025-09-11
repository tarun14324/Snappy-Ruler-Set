package com.tarun.snappyrulerset.domain.usecase

import com.tarun.snappyrulerset.domain.model.Shape
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import javax.inject.Inject

class SaveDrawingUseCase @Inject constructor(private val repo: DrawingRepository) {
    suspend operator fun invoke(shapes: List<Shape>) = repo.saveDrawing(shapes)
}