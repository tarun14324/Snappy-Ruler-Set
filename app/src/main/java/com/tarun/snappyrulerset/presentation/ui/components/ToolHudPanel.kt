package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.SetSquareVariant
import com.tarun.snappyrulerset.domain.model.state.DrawingUiState
import com.tarun.snappyrulerset.utils.extensions.format
import kotlin.math.roundToInt

/**
 * Top HUD panel for controlling active tool properties.
 * - Displays zoom level, snap status
 * - Renders tool-specific sliders (Ruler, Compass, Protractor, SetSquare)
 */
@Composable
fun ToolHudPanel(
    state: DrawingUiState,
    onRulerChange: (Float) -> Unit,
    onProtractorChange: (Float) -> Unit,
    onCompassChange: (Float) -> Unit,
    onSetSquareChange: (Float) -> Unit,
    onVariantChange: (SetSquareVariant) -> Unit,
    onSnapToggleChanged: (Boolean) -> Unit,
    onZoomChanged: (Float) -> Unit
) {

    var snapEnabled by remember { mutableStateOf(true) }
    val zoomLevel = remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC1F1F1F), shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Zoom Controls ---
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    zoomLevel.floatValue = (zoomLevel.floatValue * 1.1f).coerceIn(0f, 1.0f)
                    onZoomChanged(zoomLevel.floatValue)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2A2A2A), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    zoomLevel.floatValue = (zoomLevel.floatValue * 0.9f).coerceIn(0f, 1.0f)
                    onZoomChanged(zoomLevel.floatValue)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2A2A2A), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out",
                    tint = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Zoom: ${zoomLevel.floatValue.format(2)}x",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)

        // --- Snap Toggle ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Snap",
                color = if (snapEnabled) Color.Green else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = snapEnabled,
                onCheckedChange = { enabled ->
                    snapEnabled = enabled
                    onSnapToggleChanged(snapEnabled)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Green,
                    uncheckedThumbColor = Color.Gray
                )
            )
        }

        Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)

        // --- Tool-specific controls ---
        when (state.activeTool) {
            ActiveTool.RULER -> state.rulerState?.let {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ruler Length: ${it.length.roundToInt()} px", color = Color.White)
                    Slider(
                        value = it.length,
                        onValueChange = onRulerChange,
                        valueRange = 50f..500f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Cyan,
                            activeTrackColor = Color.Cyan.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            ActiveTool.PROTRACTOR -> state.protractorState?.let {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Protractor Angle: ${it.angle.roundToInt()}°", color = Color.White)
                    Slider(
                        value = it.angle,
                        onValueChange = onProtractorChange,
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Yellow,
                            activeTrackColor = Color.Yellow.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            ActiveTool.COMPASS -> state.compassState?.let {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Compass Radius: ${it.radius.roundToInt()} px", color = Color.White)
                    Slider(
                        value = it.radius,
                        onValueChange = onCompassChange,
                        valueRange = 20f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Magenta,
                            activeTrackColor = Color.Magenta.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            ActiveTool.SET_SQUARE -> state.setSquareState?.let {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SetSquare Size: ${it.size.roundToInt()} px", color = Color.White)
                    Slider(
                        value = it.size,
                        onValueChange = onSetSquareChange,
                        valueRange = 20f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Green,
                            activeTrackColor = Color.Green.copy(alpha = 0.7f)
                        )
                    )
                    SetSquareVariantSelector(
                        selected = it.variant,
                        onVariantSelected = onVariantChange
                    )
                }
            }

            ActiveTool.PEN -> Unit
        }
    }

}
