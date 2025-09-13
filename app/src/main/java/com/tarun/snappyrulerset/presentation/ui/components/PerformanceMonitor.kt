package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PerformanceMonitor(
    vm: DrawingViewModel,
    onCalibrationClicked: () -> Unit,
) {
    val fps by vm.fps.collectAsState()

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime > 0) {
                    val frameDuration = frameTime - lastFrameTime
                    val fps = 1_000_000_000f / frameDuration
                    vm.setFps(fps)
                }
                lastFrameTime = frameTime
            }
            delay(16L)
        }
    }


    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            "FPS: ${fps.roundToInt()}",
            color = Color.Cyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(
            modifier = Modifier.widthIn(min = 150.dp, max = 170.dp),
            onClick = { onCalibrationClicked() },
            colors = IconButtonColors(contentColor = Color.Cyan,
                containerColor = Color.Black,
                disabledContentColor = Color.Gray,
                disabledContainerColor = Color.Gray,
            )
        ) {
           Row {
               Text(
                   "Calibration",
                   color = Color.Cyan,
               )
               Spacer(Modifier.width(10.dp))
               Icon(imageVector = Icons.Default.AllInclusive, contentDescription = null,)
           }
        }
    }
}