package com.tarun.snappyrulerset.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarun.snappyrulerset.domain.model.SetSquareVariant

@Composable
fun SetSquareVariantSelector(
    selected: SetSquareVariant,
    onVariantSelected: (SetSquareVariant) -> Unit
) {
    Column {
        Text("Set Square Variant", color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == SetSquareVariant.DEG_45,
                onClick = { onVariantSelected(SetSquareVariant.DEG_45) }
            )
            Text("45°", color = Color.White)

            Spacer(Modifier.width(16.dp))

            RadioButton(
                selected = selected == SetSquareVariant.DEG_30_60,
                onClick = { onVariantSelected(SetSquareVariant.DEG_30_60) }
            )
            Text("30°–60°", color = Color.White)
        }
    }
}


