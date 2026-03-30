package com.ephemeris.helios.ui.composables.entries

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextEntryHours(
    label: String,
    modifier: Modifier = Modifier,
    time: String = "",
    duration: String = "",
    color: Color = DividerDefaults.color
) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default, color = MaterialTheme.colorScheme.onSurface)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            thickness = (1.5).dp,
            color = color
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = label, style = textStyle)
            if (time != "") Text(text = time, style = textStyle)
        }
        if (duration != "") {
            val color = DividerDefaults.color
            // The Curly Brace Separator
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp) // Space for the brace
                    .padding(vertical = 4.dp)
            ) {
                val strokeWidth = 1.dp.toPx()
                val w = size.width
                val h = size.height
                val r = 4.dp.toPx() // Curvature radius

                val path = androidx.compose.ui.graphics.Path().apply {
                    // Top curve
                    moveTo(0f, 0f)
                    quadraticTo(w * 0.5f, 0f, w * 0.5f, r)
                    // Top vertical line
                    lineTo(w * 0.5f, h * 0.5f - r)
                    // Middle point (the tip of the brace)
                    quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.5f + r)
                    // Bottom vertical line
                    lineTo(w * 0.5f, h - r)
                    // Bottom curve
                    quadraticTo(w * 0.5f, h, 0f, h)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                TextEntryVariant(duration)
            }
        }
    }
}