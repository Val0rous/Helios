package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R

@Composable
fun DurationEntry(
    title: Int,
    color: Color,
    morningStartTime: String = "",
    morningEndTime: String = "",
    morningDuration: String = "",
    eveningStartTime: String = "",
    eveningEndTime: String = "",
    eveningDuration: String = ""
) {
    HeaderEntry(text = stringResource(title), color = color)
    TextEntryHours(label = morningStartTime, time = morningEndTime, duration = morningDuration, color = color)
    TextEntryHours(label = eveningStartTime, time = eveningEndTime, duration = eveningDuration, color = color)
}
