package com.tarun.snappyrulerset.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.model.SetSquareVariant
import com.tarun.snappyrulerset.domain.model.state.CompassState
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.domain.model.state.ProtractorState
import com.tarun.snappyrulerset.domain.model.state.RulerState
import com.tarun.snappyrulerset.domain.model.state.SetSquareState
import com.tarun.snappyrulerset.domain.usecase.ExportDrawingUseCase
import com.tarun.snappyrulerset.domain.usecase.LoadDrawingUseCase
import com.tarun.snappyrulerset.domain.usecase.SaveDrawingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max


/**
 * ViewModel for managing drawing state and tool interactions.
 * Implements MVVM with StateFlow for reactive UI updates.
 */
@HiltViewModel
class DrawingViewModel @Inject constructor(
    val exportDrawingUseCase: ExportDrawingUseCase,
    val loadDrawingUseCase: LoadDrawingUseCase,
    val saveDrawingUseCase: SaveDrawingUseCase,
) : ViewModel() {

    // --- Drawing UI state ---
    private val _state = MutableStateFlow(DrawingUiState())
    val state: StateFlow<DrawingUiState> = _state.asStateFlow()

    // --- User messages (toast/snackbar) ---
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    // --- export ---
    private val _exportResult = MutableSharedFlow<ExportResult>()
    val exportResult: SharedFlow<ExportResult> = _exportResult.asSharedFlow()

    // --- Undo / Redo ---
    private val undoStack = ArrayDeque<DrawingUiState>()
    private val redoStack = ArrayDeque<DrawingUiState>()
    private val maxHistory = 20

    // --- Calibration ---
    var pxPerCm by mutableFloatStateOf(160f / 2.54f)
        private set

    // --- FPS Tracking ---
    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps

    init { load() }


    fun load() = viewModelScope.launch {
        val shapes = loadDrawingUseCase.invoke()
        _state.update { it.copy(shapes = shapes) }
    }


    /**
     * Selects the current drawing tool.
     */
    fun selectTool(tool: ActiveTool) {
        pushUndo()
        _state.update { cur ->
            when (tool) {
                ActiveTool.RULER -> cur.copy(
                    activeTool = tool,
                    rulerState = cur.rulerState ?: RulerState(Point(400f, 400f))
                )
                ActiveTool.SET_SQUARE -> cur.copy(
                    activeTool = tool,
                    setSquareState = cur.setSquareState
                        ?: SetSquareState(Point(600f, 400f), angle = 0f, size = 200f)
                )
                ActiveTool.PROTRACTOR -> cur.copy(
                    activeTool = tool,
                    protractorState = cur.protractorState
                        ?: ProtractorState(Point(500f, 600f), angle = 0f)
                )
                ActiveTool.COMPASS -> cur.copy(
                    activeTool = tool,
                    compassState = cur.compassState
                        ?: CompassState(Point(500f, 800f), radius = 0f)
                )
                else -> cur.copy(activeTool = tool)
            }
        }
        _state.value.activeTool == tool
        _state.value.currentPolyline = emptyList()
    }

    // --- Tool transformations ---
    fun moveRuler(newPos: Point) = _state.update {
        it.copy(rulerState = it.rulerState?.copy(position = newPos) ?: RulerState(newPos))
    }
    fun rotateRuler(a: Float) = _state.update {
        it.copy(rulerState = it.rulerState?.copy(angle = a))
    }

    fun moveSetSquare(newPos: Point) = _state.update {
        it.copy(setSquareState = it.setSquareState?.copy(position = newPos)
            ?: SetSquareState(newPos, angle = 0f, size = 200f))
    }
    fun rotateSetSquare(a: Float) = _state.update {
        it.copy(setSquareState = it.setSquareState?.copy(angle = a))
    }

    fun moveCompass(newCenter: Point) = _state.update {
        it.copy(compassState = it.compassState?.copy(center = newCenter)
            ?: CompassState(newCenter, radius = 0f))
    }
    fun setCompassRadius(r: Float) = _state.update {
        it.copy(compassState = it.compassState?.copy(radius = r))
    }
    fun adjustCompassRadiusBy(f: Float) = _state.update {
        val c = it.compassState ?: CompassState(Point(500f, 800f), radius = 250f)
        it.copy(compassState = c.copy(radius = (c.radius * f).coerceIn(10f, 2000f)))
    }

    fun moveProtractor(newPos: Point) = _state.update {
        it.copy(protractorState = it.protractorState?.copy(position = newPos)
            ?: ProtractorState(newPos, angle = 0f))
    }
    fun rotateProtractor(a: Float) = _state.update {
        it.copy(protractorState = it.protractorState?.copy(angle = a))
    }

    // --- Drawing actions ---
    fun onDown(p: Point) {
        pushUndo()
        _state.update { it.copy(currentPolyline = listOf(p)) }
    }
    fun onMove(p: Point) {
        _state.update { it.copy(currentPolyline = it.currentPolyline + p) }
    }
    fun onUp() {
        val cur = _state.value.currentPolyline
        if (cur.size >= 2) {
            _state.update {
                it.copy(
                    shapes = it.shapes + Polyline("LINE", cur),
                    currentPolyline = emptyList()
                )
            }
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
        } else {
            messageEmit("Nothing to undo")
        }
    }
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(_state.value)
            _state.value = next
        } else {
            messageEmit("Nothing to redo")
        }
    }

    // --- Tool property updates ---
    fun updateRulerLength(v: Float) =
        _state.update { it.copy(rulerState = it.rulerState?.copy(length = v)) }

    fun updateProtractorAngle(v: Float) =
        _state.update { it.copy(protractorState = it.protractorState?.copy(angle = v)) }

    fun updateCompassRadius(v: Float) =
        _state.update { it.copy(compassState = it.compassState?.copy(radius = v)) }

    fun updateSetSquareSize(v: Float) =
        _state.update { it.copy(setSquareState = it.setSquareState?.copy(size = v)) }

    fun updateSetSquareVariant(v: SetSquareVariant) =
        _state.update { it.copy(setSquareState = it.setSquareState?.copy(variant = v)) }


    fun saveDrawing(){
        viewModelScope.launch {
            saveDrawingUseCase.invoke(_state.value.shapes)
        }

    }
    fun exportAndShare(context: Context, format: String = "PNG") {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = try {
                exportDrawingUseCase.invoke(_state.value.shapes)
            } catch (t: Throwable) {
                null
            }
            if (uri != null) {
                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (format == "PNG") "image/png" else "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Drawing"))
                    _exportResult.emit(ExportResult.Success(uri, format))
                }
            } else {
                _exportResult.emit(ExportResult.Error("Export failed"))
            }
        }
    }

    fun setFps(value: Float) {
        _fps.value = value
    }


    fun calibrate(px: Float, realCm: Float) {
        pxPerCm = px / realCm
    }

    fun pxToCm(px: Float) = px / pxPerCm
    fun cmToPx(cm: Float) = cm * pxPerCm
    fun currentSnapRadiusPx(): Float = max(8f, 40f / _state.value.zoomLevel)

    // --- Helpers ---
    private fun pushUndo() {
        undoStack.addLast(_state.value.copy())
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun messageEmit(msg: String) {
        viewModelScope.launch { _message.emit(msg) }
    }


    // --- Export & Share ---
    sealed class ExportResult {
        data class Success(val uri: Uri, val format: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }
}
