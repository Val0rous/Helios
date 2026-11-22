package com.ephemeris.helios.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Unified corner radius and shape shared across all Helios cards and tiles. */
val CardCornerRadius = 28.dp
val CardShape = RoundedCornerShape(CardCornerRadius)

val Shapes = Shapes(
    medium = CardShape,
)
