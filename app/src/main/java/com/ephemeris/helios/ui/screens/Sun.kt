package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.ephemeris.helios.ui.composables.PathCard

@Composable
fun Sun() {
    LazyColumn() {
        // lat 0, dec 0
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(-90f, -75f, -60f, -45f, -30f, -15f, 0f, 15f, 30f, 45f, 60f, 75f, 90f, 75f, 60f, 45f, 30f, 15f, 0f, -15f, -30f, -45f, -60f, -75f, -90f)
            )
        }
        // lat 30, dec 0
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(-60f, -57.96f, -51.96f, -42.43f, -30f, -15.53f, 0f, 15.53f, 30f, 42.43f, 51.96f, 57.96f, 60f, 57.96f, 51.96f, 42.43f, 30f, 15.53f, 0f, -15.53f, -30f, -42.43f, -51.96f, -57.96f, -60f)
            )
        }
        // lat 45, dec 0
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(-45f, -43.47f, -38.97f, -31.82f, -22.5f, -11.65f, 0f, 11.65f, 22.5f, 31.82f, 38.97f, 43.47f, 45f, 43.47f, 38.97f, 31.82f, 22.5f, 11.65f, 0f, -11.65f, -22.5f, -31.82f, -38.97f, -43.47f, -45f)
            )
        }
        // lat 70, dec 0
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(-20f, -19.32f, -17.32f, -14.14f, -10f, -5.18f, 0f, 5.18f, 10f, 14.14f, 17.32f, 19.32f, 20f, 19.32f, 17.32f, 14.14f, 10f, 5.18f, 0f, -5.18f, -10f, -14.14f, -17.32f, -19.32f, -20f)
            )
        }
        // lat 80, dec 0
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(-10f, -9.66f, -8.66f, -7.07f, -5f, -2.59f, 0f, 2.59f, 5f, 7.07f, 8.66f, 9.66f, 10f, 9.66f, 8.66f, 7.07f, 5f, 2.59f, 0f, -2.59f, -5f, -7.07f, -8.66f, -9.66f, -10f)
            )
        }
        // lat 90, dec +23.44
        item {
            PathCard(
                floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f, 21f, 22f, 23f, 24f),
                floatArrayOf(23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f, 23.44f)
            )
        }
    }
}