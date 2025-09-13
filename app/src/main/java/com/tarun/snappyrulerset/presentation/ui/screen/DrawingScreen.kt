package com.tarun.snappyrulerset.presentation.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.presentation.ui.components.CalibrationDialog
import com.tarun.snappyrulerset.presentation.ui.components.CircularToolFabMenu
import com.tarun.snappyrulerset.presentation.ui.components.PerformanceMonitor
import com.tarun.snappyrulerset.presentation.ui.components.ToolHudPanel
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import com.tarun.snappyrulerset.utils.SnapEngine
import com.tarun.snappyrulerset.utils.extensions.drawCompass
import com.tarun.snappyrulerset.utils.extensions.drawProtractorWithTicks
import com.tarun.snappyrulerset.utils.extensions.drawRuler
import com.tarun.snappyrulerset.utils.extensions.drawSetSquare
import com.tarun.snappyrulerset.utils.extensions.format
import com.tarun.snappyrulerset.utils.extensions.toDegrees
import com.tarun.snappyrulerset.utils.extensions.toOffset
import com.tarun.snappyrulerset.utils.extensions.toPoint
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt


/**
 * Main Drawing Screen for the Snappy Ruler Set task.
 *
 * Features:
 * - Freehand drawing + precision geometry tools (Ruler, Set Square, Protractor, Compass)
 * - Intelligent snapping (grid, points, angles, intersections)
 * - Calibration for real-world units (cm)
 * - Precision HUD (length + angle overlay)
 * - Undo/Redo, Export (PNG/JPG)
 * - Haptic + visual snap feedback
 * - FPS overlay for performance visibility
 * - Hosts tools, canvas, snapping, HUD overlay, and export/share actions.
 */

@Composable
fun DrawingScreen(vm: DrawingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()   // Drawing state
    val context = LocalContext.current

    var snapEnabled by remember { mutableStateOf(true) }
    var snapHintPoint by remember { mutableStateOf<Point?>(null) }
    val zoomLevel = remember { mutableFloatStateOf(1f) }
    val panOffset = remember { mutableStateOf(Offset.Zero) }
    var showCalibration by remember { mutableStateOf(false) }

    // Snap hint animation
    val pulse = rememberInfiniteTransition()
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Toast messages
    LaunchedEffect(Unit) {
        vm.message.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Export results
    LaunchedEffect(Unit) {
        vm.exportResult.collectLatest { result ->
            when (result) {
                is DrawingViewModel.ExportResult.Success ->
                    Toast.makeText(context, "Exported as ${result.format}", Toast.LENGTH_SHORT).show()
                is DrawingViewModel.ExportResult.Error ->
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Calibration dialog
    if (showCalibration) {
        CalibrationDialog(
            onDismiss = { showCalibration = false },
            onConfirm = { realCm ->
                val px = state.currentPolyline.let { poly ->
                    if (poly.size >= 2) {
                        val first = poly.first()
                        val last = poly.last()
                        hypot(last.x - first.x, last.y - first.y)
                    } else 0f
                }
                if (px > 0f) vm.calibrate(px, realCm)
                showCalibration = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularToolFabMenu(vm)
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1115))
                .padding(paddingValues)
        ) {
            // --- Performance Overlay (Calibration Button) ---
            PerformanceMonitor(vm) {
                showCalibration = true
            }

            // --- Tool HUD ---
            if (state.activeTool != ActiveTool.PEN) {
                ToolHudPanel(
                    state = state,
                    onRulerChange = vm::updateRulerLength,
                    onProtractorChange = vm::updateProtractorAngle,
                    onCompassChange = vm::updateCompassRadius,
                    onSetSquareChange = vm::updateSetSquareSize,
                    onVariantChange = vm::updateSetSquareVariant,
                    onSnapToggleChanged = {
                        snapEnabled = it
                        vm.messageEmit("Snap ${if (snapEnabled) "ON" else "OFF"}")
                    },
                    onZoomChanged = { zoomLevel.floatValue = it }
                )
            }

            // --- Canvas ---
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomLevel.floatValue = (zoomLevel.floatValue * zoom).coerceIn(0.5f, 1.4f)
                                panOffset.value += pan
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val startPoint = if (snapEnabled)
                                        SnapEngine.snapPointToAll(offset.toPoint(), state, context, vm).chosen.point
                                    else offset.toPoint()
                                    vm.onDown(startPoint)
                                    snapHintPoint = startPoint
                                },
                                onDragEnd = { vm.onUp(); snapHintPoint = null },
                                onDragCancel = { vm.onUp(); snapHintPoint = null }
                            ) { change, _ ->
                                val current = if (snapEnabled)
                                    SnapEngine.snapPointToAll(change.position.toPoint(), state, context, vm).chosen.point
                                else change.position.toPoint()
                                vm.onMove(current)
                                snapHintPoint = current
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                snapEnabled = !snapEnabled
                                vm.messageEmit("Snap ${if (snapEnabled) "ON" else "OFF"}")
                            })
                        }
                        .graphicsLayer(
                            scaleX = zoomLevel.floatValue,
                            scaleY = zoomLevel.floatValue,
                            translationX = panOffset.value.x,
                            translationY = panOffset.value.y
                        )
                ) {
                    // --- Grid in calibrated cm ---
                    val gridStep = vm.cmToPx(1f) // 1 cm
                    for (x in 0..size.width.toInt() step gridStep.toInt()) {
                        drawLine(Color(0xFF1E2330), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
                    }
                    for (y in 0..size.height.toInt() step gridStep.toInt()) {
                        drawLine(Color(0xFF1E2330), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
                    }

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

                    // --- Snap hint ---
                    snapHintPoint?.let {
                        drawCircle(
                            Color.Yellow.copy(alpha = pulseAlpha),
                            10f,
                            it.toOffset()
                        )
                    }

                    // --- Precision HUD using calibrated pxPerCm ---
                    val hud = state.currentPolyline.lastOrNull()
                    if (hud != null && state.currentPolyline.size >= 2) {
                        val start = state.currentPolyline.first()
                        val dx = hud.x - start.x
                        val dy = hud.y - start.y
                        val lengthCm = vm.pxToCm(hypot(dx, dy))
                        val angleDeg = atan2(dy, dx).toDegrees().roundToInt()
                        drawContext.canvas.nativeCanvas.drawText(
                            "${lengthCm.format(2)} cm @ ${angleDeg}°",
                            hud.x + 20, hud.y - 20,
                            android.graphics.Paint().apply { color = android.graphics.Color.YELLOW; textSize = 32f }
                        )
                    }

                    // --- Tools overlay ---
                    state.rulerState?.let { drawRuler(it) }
                    state.setSquareState?.let { drawSetSquare(it) }
                    state.protractorState?.let { drawProtractorWithTicks(it) }
                    state.compassState?.let { drawCompass(it) }
                }
            }
        }
    }
}

