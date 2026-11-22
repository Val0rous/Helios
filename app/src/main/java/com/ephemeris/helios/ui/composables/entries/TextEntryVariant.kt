package com.ephemeris.helios.ui.composables.entries

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun TextEntryVariant(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = color)
    Text(text = text, style = textStyle)
}