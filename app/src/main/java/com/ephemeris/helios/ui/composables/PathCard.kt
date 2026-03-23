package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PathCard(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float = 15f
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
//            .aspectRatio(2f)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column() {
            PathChart(
                xValues = xValues,
                yValues = yValues,
                currentHour = currentHour,
                modifier = Modifier
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 0.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val textStyle = TextStyle(fontSize = (13.5).sp, fontFamily = FontFamily.Monospace)
                val verticalSpacing = 3.dp
                DividerDefaults.color
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    Text("Day Length", style = textStyle)
                    Text("12h 30m 45s", style = textStyle)
                }
                VerticalDivider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    Text("Altitude", style = textStyle)
                    Text("47.0°", style = textStyle)
                }
                VerticalDivider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    Text("Azimuth", style = textStyle)
                    Text("185.0°", style = textStyle)
                }
                VerticalDivider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(verticalSpacing)
                ) {
                    Text("Shadow Ratio", style = textStyle)
                    Text("0.94 : 1", style = textStyle)
                }
            }
        }
    }
}