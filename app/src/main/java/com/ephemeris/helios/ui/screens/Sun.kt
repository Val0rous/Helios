package com.ephemeris.helios.ui.screens

import androidx.compose.runtime.Composable
import com.ephemeris.helios.ui.composables.PathCard

@Composable
fun Sun() {
    PathCard(
        longArrayOf(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L),
        floatArrayOf(-90f, -75f, -60f, -45f, -30f, -15f, 0f, 15f, 30f, 45f, 60f, 75f, 90f, 75f, 60f, 45f, 30f, 15f, 0f, -15f, -30f, -45f, -60f, -75f, -90f)
    )
}