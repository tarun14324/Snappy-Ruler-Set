package com.tarun.snappyrulerset.usecase

import android.net.Uri
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import com.tarun.snappyrulerset.domain.usecase.ExportDrawingUseCase
import com.tarun.snappyrulerset.domain.usecase.LoadDrawingUseCase
import com.tarun.snappyrulerset.domain.usecase.SaveDrawingUseCase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UseCaseTest {

    private val repository: DrawingRepository = mockk(relaxed = true)
    private val exportUseCase = ExportDrawingUseCase(repository)
    private val saveUseCase = SaveDrawingUseCase(repository)
    private val loadUseCase = LoadDrawingUseCase(repository)

    @Test
    fun `ExportDrawingUseCase should call repository`() = runBlocking {
        val polylines = listOf(Polyline("1", listOf(Point(0f, 0f), Point(10f, 10f))))
        val expectedUri = mockk<Uri>()

        coEvery { repository.exportDrawing(polylines) } returns expectedUri

        val result = exportUseCase(polylines)

        assertEquals(expectedUri, result)
        coVerify(exactly = 1) { repository.exportDrawing(polylines) }
    }

    @Test
    fun `SaveDrawingUseCase should call repository`() = runBlocking {
        val shapes = listOf(Polyline("1", listOf(Point(0f, 0f), Point(10f, 10f))))
        coEvery { repository.saveDrawing(shapes) } returns Unit

        saveUseCase(shapes)

        coVerify(exactly = 1) { repository.saveDrawing(shapes) }
    }

    @Test
    fun `LoadDrawingUseCase should call repository and return result`() = runBlocking {
        val shapes = listOf(Polyline("1", listOf(Point(0f, 0f), Point(10f, 10f))))
        coEvery { repository.loadDrawing() } returns shapes

        val result = loadUseCase()

        assertEquals(shapes, result)
        coVerify(exactly = 1) { repository.loadDrawing() }
    }
}

