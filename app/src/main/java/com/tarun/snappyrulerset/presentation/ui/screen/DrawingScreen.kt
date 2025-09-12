package com.tarun.snappyrulerset.presentation.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.tarun.snappyrulerset.domain.model.Point
import com.tarun.snappyrulerset.domain.model.SnapType
import com.tarun.snappyrulerset.presentation.ui.components.CircularToolFabMenu
import com.tarun.snappyrulerset.presentation.ui.components.DrawingCanvas
import com.tarun.snappyrulerset.presentation.ui.components.ToolHudPanel
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DrawingScreen(vm: DrawingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var snapEnabled by remember { mutableStateOf(true) }
    var snapHintPoint by remember { mutableStateOf<Point?>(null) }
    var snapType by remember { mutableStateOf(SnapType.NONE) }
    var lastSnapType by remember { mutableStateOf(SnapType.NONE) }

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

    // Share flow
    LaunchedEffect(Unit) {
        vm.share.collectLatest { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Drawing"))
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
            // --- Top HUD Panel ---
            ToolHudPanel(
                state = state,
                zoomLevel = zoomLevel.floatValue,
                snapEnabled = snapEnabled,
                snapType = snapType,
                onRulerChange = vm::updateRulerLength,
                onProtractorChange = vm::updateProtractorAngle,
                onCompassChange = vm::updateCompassRadius,
                onSetSquareChange = vm::updateSetSquareSize
            )

            // --- Canvas ---
            DrawingCanvas(
                state = state,
                zoomLevel = zoomLevel,
                snapEnabled = snapEnabled,
                snapHintPoint = snapHintPoint,
                snapType = snapType,
                onSnapUpdate = { point, type ->
                    snapHintPoint = point
                    snapType = type
                    if (type != SnapType.NONE && type != lastSnapType) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    lastSnapType = type
                },
                pulseAlpha = pulseAlpha,
                onToggleSnap = {
                    snapEnabled = !snapEnabled
                    vm.messageEmit("Snap ${if (snapEnabled) "ON" else "OFF"}")
                },
                vm = vm,
                context = context
            )
        }
    }
}

