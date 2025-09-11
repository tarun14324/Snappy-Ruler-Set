package com.tarun.snappyrulerset.presentation.viewmodel

import android.content.Context
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

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    private val undoStack = ArrayDeque<DrawingUiState>()
    private val redoStack = ArrayDeque<DrawingUiState>()
    private val maxHistory = 20

    // zoom + pan used to derive snap radius; expose zoom for other modules if needed
    private var _zoom = 1f
    val zoom get() = _zoom
    private var panX = 0f
    private var panY = 0f

    // --- Tool selection + initialization ---
    fun selectTool(tool: ActiveTool) {
        pushUndo()
        _state.update { cur ->
            when (tool) {
                ActiveTool.RULER -> cur.copy(activeTool = tool, rulerState = cur.rulerState ?: RulerState(Point(400f, 400f), angle = 0F))
                ActiveTool.SET_SQUARE -> cur.copy(activeTool = tool, setSquareState = cur.setSquareState ?: SetSquareState(Point(600f, 400f), angle = 0F, size = 200F))
                ActiveTool.PROTRACTOR -> cur.copy(activeTool = tool, protractorState = cur.protractorState ?: ProtractorState(Point(500f, 600f), angle = 0F))
                ActiveTool.COMPASS -> cur.copy(activeTool = tool, compassState = cur.compassState ?: CompassState(Point(500f,800f), radius = 100f))
                else -> cur.copy(activeTool = tool)
            }
        }
    }

    // --- Ruler / SetSquare ---
    fun moveRuler(newPos: Point) = _state.update { it.copy(rulerState = it.rulerState?.copy(position = newPos) ?: RulerState(newPos)) }
    fun rotateRuler(newAngle: Float) = _state.update { it.copy(rulerState = it.rulerState?.copy(angle = newAngle)) }
    fun moveSetSquare(newPos: Point) = _state.update { it.copy(setSquareState = it.setSquareState?.copy(position = newPos) ?: SetSquareState(newPos, angle = 0F, size = 200F)) }
    fun rotateSetSquare(newAngle: Float) = _state.update { it.copy(setSquareState = it.setSquareState?.copy(angle = newAngle)) }

    // --- Compass adjustments (new) ---
    fun moveCompass(newCenter: Point) = _state.update { it.copy(compassState = it.compassState?.copy(center = newCenter) ?: CompassState(newCenter, radius = 100f) )}
    fun setCompassRadius(newRadius: Float) = _state.update { it.copy(compassState = it.compassState?.copy(radius = newRadius)) }
    fun adjustCompassRadiusBy(factor: Float) {
        _state.update {
            val current = it.compassState ?: CompassState(Point(500f, 800f), radius = 100f)
            val next = (current.radius * factor).coerceIn(10f, 2000f)
            it.copy(compassState = current.copy(radius = next))
        }
    }

    // --- Protractor adjustments (we keep rotate and position) ---
    fun moveProtractor(newPos: Point) = _state.update { it.copy(protractorState = it.protractorState?.copy(position = newPos) ?: ProtractorState(newPos, angle = 0F)) }
    fun rotateProtractor(newAngle: Float) = _state.update { it.copy(protractorState = it.protractorState?.copy(angle = newAngle)) }

    // --- Zoom / Pan ---
    fun zoomBy(factor: Float) {
        _state.update { current ->
            current.copy(zoomLevel = current.zoomLevel * factor)
        }
    }
    fun panBy(dx: Float, dy: Float) { panX += dx; panY += dy }

    // --- Drawing ---
    fun onDown(p: Point) { pushUndo(); _state.update { it.copy(currentPolyline = listOf(p)) } }
    fun onMove(p: Point) { _state.update { it.copy(currentPolyline = it.currentPolyline + p) } }
    fun onUp() {
        val cur = _state.value.currentPolyline
        if (cur.size >= 2) {
            _state.update { it.copy(shapes = it.shapes + Polyline("LINE",cur), currentPolyline = emptyList()) }
        } else {
            _state.update { it.copy(currentPolyline = emptyList()) }
        }
    }

    // --- Undo / Redo ---
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

    fun updateRulerLength(newLength: Float) {
        _state.update { current ->
            val ruler = current.rulerState
            if (ruler != null) current.copy(
                rulerState = ruler.copy(length = newLength)
            ) else current
        }
    }

    fun updateProtractorAngle(newAngle: Float) {
        _state.update { current ->
            val protractor = current.protractorState
            if (protractor != null) current.copy(
                protractorState = protractor.copy(angle = newAngle)
            ) else current
        }
    }

    fun updateCompassRadius(newRadius: Float) {
        _state.update { current ->
            val compass = current.compassState
            if (compass != null) current.copy(
                compassState = compass.copy(radius = newRadius)
            ) else current
        }
    }

    fun updateSetSquareSize(newSize: Float) {
        _state.update { current ->
            val setSquare = current.setSquareState
            if (setSquare != null) current.copy(
                setSquareState = setSquare.copy(size = newSize)
            ) else current
        }
    }

    private fun pushUndo() {
        undoStack.addLast(_state.value.copy())
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    // dynamic snap radius (uses current zoom)
    fun currentSnapRadiusPx(): Float = max(8f, 40f / _zoom)

    fun messageEmit(msg: String) = viewModelScope.launch { _message.emit(msg) }

    // --- Export ---
    suspend fun exportBitmap(): Uri? {
        return repo.exportDrawing(_state.value.shapes).apply {
            messageEmit("Exported")
        }
    }
}
