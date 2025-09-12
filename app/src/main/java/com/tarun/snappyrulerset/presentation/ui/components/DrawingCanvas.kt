package com.tarun.snappyrulerset.presentation.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.SnapResult
import com.tarun.snappyrulerset.domain.model.SnapType
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import com.tarun.snappyrulerset.utils.SnapEngine
import com.tarun.snappyrulerset.utils.extensions.drawCompass
import com.tarun.snappyrulerset.utils.extensions.drawProtractorWithTicks
import com.tarun.snappyrulerset.utils.extensions.drawRuler
import com.tarun.snappyrulerset.utils.extensions.drawSetSquare
import com.tarun.snappyrulerset.utils.extensions.toDegrees
import com.tarun.snappyrulerset.utils.extensions.toOffset
import com.tarun.snappyrulerset.utils.extensions.toPoint

@Composable
fun DrawingCanvas(
    state: DrawingUiState,
    zoomLevel: MutableState<Float>,
    snapEnabled: Boolean,
    snapHintPoint: Point?,
    snapType: SnapType,
    onSnapUpdate: (Point?, SnapType) -> Unit,
    pulseAlpha: Float,
    onToggleSnap: () -> Unit,
    vm: DrawingViewModel,
    context: Context
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    state.activeTool,
                    state.rulerState,
                    state.setSquareState,
                    state.compassState,
                    state.protractorState
                ) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        zoomLevel.value *= zoom
                        when (state.activeTool) {
                            ActiveTool.RULER -> state.rulerState?.let {
                                vm.moveRuler(Point(it.position.x + pan.x, it.position.y + pan.y))
                                vm.rotateRuler(it.angle + rotation.toDegrees())
                            }
                            ActiveTool.SET_SQUARE -> state.setSquareState?.let {
                                vm.moveSetSquare(Point(it.position.x + pan.x, it.position.y + pan.y))
                                vm.rotateSetSquare(it.angle + rotation.toDegrees())
                            }
                            ActiveTool.COMPASS -> state.compassState?.let {
                                vm.moveCompass(Point(it.center.x + pan.x, it.center.y + pan.y))
                                if (zoom != 1f) vm.adjustCompassRadiusBy(zoom)
                            }
                            ActiveTool.PROTRACTOR -> state.protractorState?.let {
                                vm.moveProtractor(Point(it.position.x + pan.x, it.position.y + pan.y))
                                vm.rotateProtractor(it.angle + rotation.toDegrees())
                            }
                            else -> vm.panBy(pan.x, pan.y)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o ->
                            val snapResult = if (snapEnabled) {
                                SnapEngine.snapPointToAll(o.toPoint(), state, context, vm)
                            } else SnapResult(o.toPoint(), SnapType.NONE)

                            if (state.activeTool == ActiveTool.PEN) vm.onDown(snapResult.point)

                            onSnapUpdate(snapResult.point, snapResult.type)
                        },
                        onDragEnd = {
                            if (state.activeTool == ActiveTool.PEN) vm.onUp()
                            onSnapUpdate(null, SnapType.NONE)
                        },
                        onDragCancel = {
                            if (state.activeTool == ActiveTool.PEN) vm.onUp()
                            onSnapUpdate(null, SnapType.NONE)
                        }
                    ) { change, _ ->
                        val snapResult = if (snapEnabled) {
                            SnapEngine.snapPointToAll(change.position.toPoint(), state, context, vm)
                        } else SnapResult(change.position.toPoint(), SnapType.NONE)

                        if (state.activeTool == ActiveTool.PEN) vm.onMove(snapResult.point)

                        onSnapUpdate(snapResult.point, snapResult.type)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onToggleSnap() })
                }
        ) {
            // --- Grid ---
            val step = 20f
            for (x in 0..size.width.toInt() step step.toInt())
                drawLine(Color(0xFF1E2330), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
            for (y in 0..size.height.toInt() step step.toInt())
                drawLine(Color(0xFF1E2330), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))

            // --- Saved shapes ---
            state.shapes.forEach { shape ->
                shape.points.windowed(2).forEach { (a, b) ->
                    drawLine(Color(0xFF66D9EF), a.toOffset(), b.toOffset(), 3f)
                }
            }

            // --- Current polyline ---
            state.currentPolyline.windowed(2).forEach { (a, b) ->
                drawLine(Color(0xFFA6E22E), a.toOffset(), b.toOffset(), 5f)
            }

            // --- Snap hint (uses snapType now) ---
            snapHintPoint?.let {
                val color = when (snapType) {
                    SnapType.POINT -> Color.Cyan
                    SnapType.ANGLE -> Color.Magenta
                    SnapType.GRID -> Color.Yellow
                    else -> Color.Transparent
                }
                drawCircle(color.copy(alpha = pulseAlpha), 10f, it.toOffset())
            }

            // --- Tools (only active one) ---
            when (state.activeTool) {
                ActiveTool.RULER -> state.rulerState?.let { drawRuler(it) }
                ActiveTool.SET_SQUARE -> state.setSquareState?.let { drawSetSquare(it) }
                ActiveTool.PROTRACTOR -> state.protractorState?.let { drawProtractorWithTicks(it) }
                ActiveTool.COMPASS -> state.compassState?.let { drawCompass(it) }
                else -> {}
            }
        }
    }
}