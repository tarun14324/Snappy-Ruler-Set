package com.tarun.snappyrulerset.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarun.snappyrulerset.domain.model.DrawingUiState
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.domain.model.Shape
import com.tarun.snappyrulerset.domain.snap.SnapEngine
import com.tarun.snappyrulerset.domain.usecase.LoadDrawingUseCase
import com.tarun.snappyrulerset.domain.usecase.SaveDrawingUseCase
import com.tarun.snappyrulerset.utils.UndoRedo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID


@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val saveDrawing: SaveDrawingUseCase,
    private val loadDrawing: LoadDrawingUseCase,
    private val snapEngine: SnapEngine,
) : ViewModel() {


    private val _state = MutableStateFlow(DrawingUiState())
    val state: StateFlow<DrawingUiState> = _state


    private val history = UndoRedo<DrawingUiState>()


    init { load() }


    fun load() = viewModelScope.launch {
        val shapes = loadDrawing()
        _state.update { it.copy(shapes = shapes) }
    }

    fun save() = viewModelScope.launch { saveDrawing(_state.value.shapes) }

    fun onDown(raw: Point) {
        history.push(_state.value)
        _state.update { it.copy(currentPolyline = listOf(raw)) }
    }


    fun onMove(raw: Point) {
        val snapped =
            snapEngine.snap(
                raw = raw,
                zoom = _state.value.zoom,
                points = collectSnapPoints(),
                angleLine = null
            )
        _state.update {
            val pts = it.currentPolyline + snapped.snapped
            it.copy(
                currentPolyline = pts,
                hudText = "len: ${pts.size}"
            )
        }
    }


    fun onUp() {
        val pts = _state.value.currentPolyline
        if (pts.size >= 2) {
            val shape: Shape = Polyline(id = UUID.randomUUID().toString(), points = pts)
            _state.update { it.copy(shapes = it.shapes + shape, currentPolyline = emptyList()) }
        } else {
            _state.update { it.copy(currentPolyline = emptyList()) }
        }
    }


    fun undo() { history.undo(_state.value)?.let { _state.value = it } }
    fun redo() { history.redo(_state.value)?.let { _state.value = it } }
    fun zoomBy(f: Float) { _state.update { it.copy(zoom = (it.zoom * f).coerceIn(0.25f, 4f)) } }
    fun panBy(dx: Float, dy: Float) { _state.update { it.copy(panX = it.panX + dx, panY = it.panY + dy) } }


    private fun collectSnapPoints(): List<Point> = buildList {
        _state.value.shapes.forEach { shape ->
            when (shape) {
                is Polyline -> {
                    addAll(shape.points)
                    shape.points.windowed(2).forEach { pair -> add(midPoint(pair[0], pair[1])) }
                }
                else -> Unit
            }
        }
    }


    private fun midPoint(a: Point, b: Point) = Point((a.x + b.x) / 2f, (a.y + b.y) / 2f)
}