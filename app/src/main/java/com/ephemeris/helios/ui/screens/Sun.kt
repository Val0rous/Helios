package com.ephemeris.helios.ui.screens

import androidx.compose.runtime.Composable
import com.ephemeris.helios.ui.composables.PathCard

@Composable
fun Sun() {
    PathCard(
        floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
        floatArrayOf(-90f, -75f, -60f, -45f, -30f, -15f, 0f, 15f, 30f, 45f, 60f, 75f, 90f, 75f, 60f, 45f, 30f, 15f, 0f, -15f, -30f, -45f, -60f, -75f, -90f)
    )
}