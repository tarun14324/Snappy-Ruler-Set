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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.domain.model.ActiveTool
import com.tarun.snappyrulerset.domain.model.FabMenuItem
import com.tarun.snappyrulerset.presentation.viewmodel.DrawingViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularToolFabMenu(
    vm: DrawingViewModel,
    radius: Float = 150f,
    itemSize: Dp = 48.dp,
    mainFabSize: Dp = 56.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        FabMenuItem(Icons.Default.Straighten, "Ruler") {
            vm.selectTool(ActiveTool.RULER)
        },
        FabMenuItem(Icons.Default.ChangeHistory, "Set Square") {
            vm.selectTool(ActiveTool.SET_SQUARE)
        },
        FabMenuItem(Icons.Default.DonutLarge, "Protractor") {
            vm.selectTool(ActiveTool.PROTRACTOR)
        },
        FabMenuItem(Icons.Default.Circle, "Compass") {
            vm.selectTool(ActiveTool.COMPASS)
        },
        FabMenuItem(Icons.Default.Edit, "Pen") {
            vm.selectTool(ActiveTool.PEN)
        },
        FabMenuItem(Icons.AutoMirrored.Filled.Undo, "Undo") { vm.undo() },
        FabMenuItem(Icons.AutoMirrored.Filled.Redo, "Redo") { vm.redo() },
        FabMenuItem(Icons.Default.Save, "Save") {
            scope.launch {
                vm.exportBitmap()
                vm.messageEmit("Exported")
            }
        },
        FabMenuItem(Icons.Default.Share, "Share") {
            scope.launch { vm.share() }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        menuItems.forEachIndexed { index, item ->
            val angle = 180f / (menuItems.size - 1) * index

            val targetX = if (expanded) -cos(Math.toRadians(angle.toDouble())).toFloat() * radius else 0f
            val targetY = if (expanded) -sin(Math.toRadians(angle.toDouble())).toFloat() * radius else 0f

            val offsetX by animateFloatAsState(targetValue = targetX)
            val offsetY by animateFloatAsState(targetValue = targetY)

            FloatingActionButton(
                onClick = {
                    item.onClick()
                    expanded = false // collapse after selection
                },
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(itemSize)
                    .alpha(if (expanded) 1f else 0f)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = Color.White)
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .padding(16.dp)
                .size(mainFabSize)
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close" else "Menu"
            )
        }
    }
}

