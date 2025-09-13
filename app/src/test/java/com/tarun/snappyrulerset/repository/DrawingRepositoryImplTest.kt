package com.tarun.snappyrulerset.repository

import android.content.Context
import com.tarun.snappyrulerset.data.repository.DrawingRepositoryImpl
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawingRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var repo: DrawingRepositoryImpl
    private lateinit var filesDir: File
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        filesDir = createTempDir()
        cacheDir = createTempDir()

        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns cacheDir
        every { context.packageName } returns "com.example.test"

        repo = DrawingRepositoryImpl(context)
    }

    @After
    fun teardown() {
        filesDir.deleteRecursively()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `saveDrawing should create a JSON file`() = runBlocking {
        val polyline = Polyline("1", listOf(Point(0f, 0f), Point(10f, 10f)))
        repo.saveDrawing(listOf(polyline))

        val file = File(filesDir, "drawing.json")
        assertTrue(file.exists(), "drawing.json file should exist")
        assertTrue(file.readText().contains(polyline.id), "File content should contain polyline id")
    }

    @Test
    fun `loadDrawing should return saved polylines`() = runBlocking {
        val polyline = Polyline("1", listOf(Point(0f, 0f), Point(10f, 10f)))
        repo.saveDrawing(listOf(polyline))

        val loaded = repo.loadDrawing()
        assertEquals(1, loaded.size, "Should load one polyline")
        assertEquals(polyline.id, loaded[0].id, "Loaded polyline id should match")
        assertEquals(polyline.points, loaded[0].points, "Loaded points should match")
    }

}
