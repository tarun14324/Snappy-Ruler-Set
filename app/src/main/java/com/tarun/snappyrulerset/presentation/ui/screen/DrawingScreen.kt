package com.tarun.snappyrulerset.presentation.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.Polyline
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(vm: DrawingViewModel = hiltViewModel()) {
    val state = vm.state
    val s = state.collectAsState().value
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Snappy Ruler Set") },
                actions = {
                    IconButton({ vm.undo() }) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "undo"
                        )
                    }
                    IconButton({ vm.redo() }) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "redo"
                        )
                    }
                    IconButton({ scope.launch { vm.save() } }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "save"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: export bitmap */ }) {
                Text("Export")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F1115))
        ) {
            val density = LocalDensity.current


            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(s.zoom, s.panX, s.panY) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            vm.zoomBy(zoom)
                            vm.panBy(pan.x, pan.y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { o -> vm.onDown(o.toPoint()) },
                            onDragEnd = { vm.onUp() },
                            onDragCancel = { vm.onUp() }
                        ) { change, _ ->
                            vm.onMove(change.position.toPoint())
                        }
                    }
            ) {
// Draw grid
                val step = 20f
                val w = size.width
                val h = size.height
                for (x in 0..w.toInt() step step.toInt()) {
                    drawLine(Color(0xFF1E2330), Offset(x.toFloat(), 0f), Offset(x.toFloat(), h))
                }
                for (y in 0..h.toInt() step step.toInt()) {
                    drawLine(Color(0xFF1E2330), Offset(0f, y.toFloat()), Offset(w, y.toFloat()))
                }
// Draw shapes
                s.shapes.forEach { shape ->
                    when (shape) {
                        is Polyline -> shape.points.windowed(2).forEach { (a, b) ->
                            drawLine(
                                color = Color(0xFF66D9EF),
                                start = a.toOffset(),
                                end = b.toOffset(),
                                strokeWidth = 3f
                            )
                        }

                        else -> Unit
                    }
                }


// Current polyline
                s.currentPolyline.windowed(2).forEach { (a, b) ->
                    drawLine(
                        color = Color(0xFFA6E22E),
                        start = a.toOffset(),
                        end = b.toOffset(),
                        strokeWidth = 3f
                    )
                }
            }


// HUD
            Text(
                text = s.hudText,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun Offset.toPoint() = Point(x, y)
private fun Point.toOffset() = Offset(x, y)
