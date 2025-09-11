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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.presentation.ui.components.CircularToolFabMenu
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

@Composable
fun DrawingScreen(vm: DrawingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var snapEnabled by remember { mutableStateOf(true) }
    var snapHintPoint by remember { mutableStateOf<Point?>(null) }
    val zoomLevel = remember { mutableFloatStateOf(1f) }

    // Snap hint animation
    val pulse = rememberInfiniteTransition()
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
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

    Scaffold(
        floatingActionButton = { CircularToolFabMenu(vm) },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1115))
                .padding(paddingValues)
        ) {
            // --- Top Tool Property Panel ---
            if (state.activeTool != ActiveTool.PEN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x88000000))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Zoom: ${zoomLevel.floatValue.format(2)}x", color = Color.White)
                    Text(
                        "Snap: ${if (snapEnabled) "ON" else "OFF"}",
                        color = if (snapEnabled) Color.Green else Color.Gray
                    )

                    when (state.activeTool) {
                        ActiveTool.RULER -> state.rulerState?.let {
                            Text("Ruler Length: ${it.length.toInt()} px", color = Color.White)
                            Slider(
                                value = it.length,
                                onValueChange = { vm.updateRulerLength(it) },
                                valueRange = 50f..500f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ActiveTool.PROTRACTOR -> state.protractorState?.let {
                            Text("Protractor Angle: ${it.angle.toInt()}°", color = Color.White)
                            Slider(
                                value = it.angle,
                                onValueChange = { vm.updateProtractorAngle(it) },
                                valueRange = 0f..360f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ActiveTool.COMPASS -> state.compassState?.let {
                            Text("Compass Radius: ${it.radius.toInt()} px", color = Color.White)
                            Slider(
                                value = it.radius,
                                onValueChange = { vm.updateCompassRadius(it) },
                                valueRange = 20f..300f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ActiveTool.SET_SQUARE -> state.setSquareState?.let {
                            Text("Set Square Size: ${it.size.toInt()} px", color = Color.White)
                            Slider(
                                value = it.size,
                                onValueChange = { vm.updateSetSquareSize(it) },
                                valueRange = 20f..200f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {}
                    }
                }
            }

            // --- Canvas occupies remaining space ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.activeTool, state.rulerState, state.setSquareState, state.compassState, state.protractorState) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                zoomLevel.floatValue *= zoom
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
                                    val startPoint = o.toPoint()
                                    val snapped = if (snapEnabled) SnapEngine.snapPointToAll(startPoint, state, context, vm) else startPoint
                                    vm.onDown(snapped)
                                    snapHintPoint = snapped
                                },
                                onDragEnd = { vm.onUp(); snapHintPoint = null },
                                onDragCancel = { vm.onUp(); snapHintPoint = null }
                            ) { change, _ ->
                                val current = change.position.toPoint()
                                val snapped = if (snapEnabled) SnapEngine.snapPointToAll(current, state, context, vm) else current
                                vm.onMove(snapped)
                                snapHintPoint = snapped
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                snapEnabled = !snapEnabled
                                vm.messageEmit("Snap ${if (snapEnabled) "ON" else "OFF"}")
                            })
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

                    // --- Snap hint ---
                    snapHintPoint?.let { drawCircle(Color.Yellow.copy(alpha = pulseAlpha), 10f, it.toOffset()) }

                    // --- Tools ---
                    state.rulerState?.let { drawRuler(it) }
                    state.setSquareState?.let { drawSetSquare(it) }
                    state.protractorState?.let { drawProtractorWithTicks(it) }
                    state.compassState?.let { drawCompass(it) }
                }
            }
        }
    }
}
