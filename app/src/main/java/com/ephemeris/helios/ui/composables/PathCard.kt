package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PathCard(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float = 15f
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    ) {
        PathChart(
            xValues = xValues,
            yValues = yValues,
            currentHour = currentHour,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(vertical = 0.dp))
        )
    }
}