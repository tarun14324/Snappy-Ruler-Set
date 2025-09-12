package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.SnapType
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.utils.extensions.format

@Composable
fun ToolHudPanel(
    state: DrawingUiState,
    zoomLevel: Float,
    snapEnabled: Boolean,
    snapType: SnapType,
    onRulerChange: (Float) -> Unit,
    onProtractorChange: (Float) -> Unit,
    onCompassChange: (Float) -> Unit,
    onSetSquareChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x88000000))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Zoom: ${zoomLevel.format(2)}x", color = Color.White)
        Text(
            "Snap: ${if (snapEnabled) "ON" else "OFF"} (${snapType.name})",
            color = if (snapEnabled) Color.Green else Color.Gray
        )

        when (state.activeTool) {
            ActiveTool.RULER -> state.rulerState?.let {
                Text("Ruler Length: ${it.length.toInt()} px", color = Color.White)
                Slider(
                    value = it.length,
                    onValueChange = onRulerChange,
                    valueRange = 50f..500f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActiveTool.PROTRACTOR -> state.protractorState?.let {
                Text("Protractor Angle: ${it.angle.toInt()}°", color = Color.White)
                Slider(
                    value = it.angle,
                    onValueChange = onProtractorChange,
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActiveTool.COMPASS -> state.compassState?.let {
                Text("Compass Radius: ${it.radius.toInt()} px", color = Color.White)
                Slider(
                    value = it.radius,
                    onValueChange = onCompassChange,
                    valueRange = 20f..300f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            ActiveTool.SET_SQUARE -> state.setSquareState?.let {
                Text("Set Square Size: ${it.size.toInt()} px", color = Color.White)
                Slider(
                    value = it.size,
                    onValueChange = onSetSquareChange,
                    valueRange = 20f..200f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {}
        }
    }
}