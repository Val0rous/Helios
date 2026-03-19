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
    xValuesMillis: LongArray,
    yValues: FloatArray,
    currentTimeMillis: Long = 15L
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    ) {
        PathChart(
            xValuesMillis = xValuesMillis,
            yValues = yValues,
            currentTimeMillis = currentTimeMillis,
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(vertical = 0.dp))
        )
    }
}