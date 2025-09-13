package com.tarun.snappyrulerset.domain.model

import androidx.compose.ui.graphics.vector.ImageVector

data class FabMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)