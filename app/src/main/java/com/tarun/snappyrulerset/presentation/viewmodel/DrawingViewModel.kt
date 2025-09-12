package com.tarun.snappyrulerset.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.snappyrulerset.domain.model.*
import com.tarun.snappyrulerset.domain.model.state.*
import com.tarun.snappyrulerset.domain.repository.DrawingRepository
import com.tarun.snappyrulerset.domain.usecase.ExportDrawingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val exportUseCase: ExportDrawingUseCase,
    private val repo: DrawingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DrawingUiState())
    val state: StateFlow<DrawingUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message

    private val _share = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val share: SharedFlow<Uri> = _share

    private val undoStack = ArrayDeque<DrawingUiState>()
    private val redoStack = ArrayDeque<DrawingUiState>()
    private val maxHistory = 20

    private var _zoom = 1f
    val zoom: Float get() = _zoom
    private var panX = 0f
    private var panY = 0f

    // --- Tool selection ---
    fun selectTool(tool: ActiveTool) {
        pushUndo()
        _state.update { cur ->
            when (tool) {
                ActiveTool.RULER -> cur.copy(activeTool = tool, rulerState = cur.rulerState ?: RulerState(Point(400f, 400f), 0f))
                ActiveTool.SET_SQUARE -> cur.copy(activeTool = tool, setSquareState = cur.setSquareState ?: SetSquareState(Point(600f, 400f), 0f, size = 200f))
                ActiveTool.PROTRACTOR -> cur.copy(activeTool = tool, protractorState = cur.protractorState ?: ProtractorState(Point(500f, 600f), angle = 0f))
                ActiveTool.COMPASS -> cur.copy(activeTool = tool, compassState = cur.compassState ?: CompassState(Point(500f, 800f), 100f))
                else -> cur.copy(activeTool = tool)
            }
        }
    }

    // --- HUD update functions (used by ToolHudPanel) ---
    fun updateRulerLength(newLength: Float) = updateTool {
        it.copy(rulerState = it.rulerState?.copy(length = newLength))
    }

    fun updateProtractorAngle(newAngle: Float) = updateTool {
        it.copy(protractorState = it.protractorState?.copy(angle = newAngle))
    }

    fun updateCompassRadius(newRadius: Float) = updateTool {
        it.copy(compassState = it.compassState?.copy(radius = newRadius))
    }

    fun updateSetSquareSize(newSize: Float) = updateTool {
        it.copy(setSquareState = it.setSquareState?.copy(size = newSize))
    }

    // --- Tool moves / transformations ---
    fun moveRuler(newPos: Point) = updateTool {
        it.copy(rulerState = it.rulerState?.copy(position = newPos) ?: RulerState(newPos))
    }
    fun rotateRuler(newAngle: Float) = updateTool {
        it.copy(rulerState = it.rulerState?.copy(angle = newAngle))
    }

    fun moveSetSquare(newPos: Point) = updateTool {
        it.copy(setSquareState = it.setSquareState?.copy(position = newPos) ?: SetSquareState(newPos, 0f, size = 200f))
    }
    fun rotateSetSquare(newAngle: Float) = updateTool {
        it.copy(setSquareState = it.setSquareState?.copy(angle = newAngle))
    }

    fun moveCompass(newCenter: Point) = updateTool {
        it.copy(compassState = it.compassState?.copy(center = newCenter) ?: CompassState(newCenter, 100f))
    }
    fun adjustCompassRadiusBy(factor: Float) = updateTool {
        val current = it.compassState ?: CompassState(Point(500f, 800f), 100f)
        val next = (current.radius * factor).coerceIn(10f, 2000f)
        it.copy(compassState = current.copy(radius = next))
    }

    fun moveProtractor(newPos: Point) = updateTool {
        it.copy(protractorState = it.protractorState?.copy(position = newPos) ?: ProtractorState(newPos, angle = 0f))
    }
    fun rotateProtractor(newAngle: Float) = updateTool {
        it.copy(protractorState = it.protractorState?.copy(angle = newAngle))
    }

    // --- Drawing (Pen / Polyline) ---
    fun onDown(p: Point) {
        pushUndo()
        _state.update { it.copy(currentPolyline = listOf(p)) }
    }

    fun onMove(p: Point) = _state.update {
        it.copy(currentPolyline = it.currentPolyline + p)
    }

    fun onUp() {
        val cur = _state.value.currentPolyline
        _state.update {
            if (cur.size >= 2) it.copy(
                shapes = it.shapes + Polyline("LINE", cur),
                currentPolyline = emptyList()
            ) else it.copy(currentPolyline = emptyList())
        }
    }

    // --- Undo/Redo ---
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeLast()
            redoStack.addLast(_state.value)
            _state.value = prev
        } else messageEmit("Nothing to undo")
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(_state.value)
            _state.value = next
        } else messageEmit("Nothing to redo")
    }

    private fun pushUndo() {
        undoStack.addLast(_state.value.copy())
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    // --- Zoom / Pan ---
    fun zoomBy(factor: Float) {
        _zoom *= factor
        _state.update { cur -> cur.copy(zoomLevel = cur.zoomLevel * factor) }
    }

    fun panBy(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun currentSnapRadiusPx(): Float = max(8f, 40f / _zoom)

    // --- UI Events ---
    fun messageEmit(msg: String) = viewModelScope.launch { _message.emit(msg) }

    // --- Export & Share ---
    suspend fun exportBitmap(): Uri? = repo.exportDrawing(_state.value.shapes)

    suspend fun share() {
        exportBitmap()?.let { _share.emit(it) }
    }

    // --- Utility ---
    private inline fun updateTool(update: (DrawingUiState) -> DrawingUiState) {
        _state.update(update)
    }
}