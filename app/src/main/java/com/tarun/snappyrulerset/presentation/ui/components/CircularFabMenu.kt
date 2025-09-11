package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularToolFabMenu(
    vm: DrawingViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun selectedTool(tool: ActiveTool) {
        vm.selectTool(tool)
        expanded = !expanded
    }

    // List of tools
    val menuItems = listOf(
        Triple(Icons.Default.Straighten, ActiveTool.RULER) {
            selectedTool(ActiveTool.RULER)
        },
        Triple(
            Icons.Default.ChangeHistory,
            ActiveTool.SET_SQUARE
        ) { selectedTool(ActiveTool.SET_SQUARE) },
        Triple(
            Icons.Default.DonutLarge,
            ActiveTool.PROTRACTOR
        ) {
            selectedTool(ActiveTool.PROTRACTOR)
        },
        Triple(Icons.Default.Circle, ActiveTool.COMPASS) { selectedTool(ActiveTool.COMPASS) },
        Triple(Icons.Default.Edit, ActiveTool.PEN) { selectedTool(ActiveTool.PEN) },
        Triple(Icons.AutoMirrored.Filled.Undo, null) { vm.undo() },
        Triple(Icons.AutoMirrored.Filled.Redo, null) { vm.redo() },
        Triple(Icons.Default.Save, null) { scope.launch { vm.exportBitmap() } }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        menuItems.forEachIndexed { index, item ->
            val angle = 180f / (menuItems.size - 1) * index
            val offsetX by animateFloatAsState(
                targetValue = if (expanded) -cos(Math.toRadians(angle.toDouble())).toFloat() * 150f else 0f
            )
            val offsetY by animateFloatAsState(
                targetValue = if (expanded) -sin(Math.toRadians(angle.toDouble())).toFloat() * 150f else 0f
            )

            FloatingActionButton(
                onClick = { item.third() },
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(48.dp)
                    .alpha(if (expanded) 1f else 0f)
            ) {
                Icon(item.first, contentDescription = null, tint = Color.White)
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = null
            )
        }
    }
}

