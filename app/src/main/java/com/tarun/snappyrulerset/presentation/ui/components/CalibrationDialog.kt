package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun CalibrationDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var userInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                userInput.toFloatOrNull()?.let { cm ->
                    onConfirm(cm)
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Calibration") },
        text = {
            Column {
                Text("Draw a line on screen that matches your physical ruler (e.g. 10 cm).")
                TextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Enter actual length in cm") }
                )
            }
        }
    )
}
