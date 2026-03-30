package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.utils.formatDuration
import java.time.ZonedDateTime

@Composable
fun SeasonalEntry(
    title: Int,
    color: Color,
    dateTimeTop: ZonedDateTime,
    daylightTop: Double,
    sunAngleTop: Double,
    dateTimeBottom: ZonedDateTime,
    daylightBottom: Double,
    sunAngleBottom: Double
) {
    val year = dateTimeTop.year
    HeaderEntry(text = "${stringResource(title)} (${year})", color = color)
    TextEntrySeasons(dateTime = dateTimeTop, daylight = daylightTop.formatDuration(), sunAngle = sunAngleTop, color = color)
    TextEntrySeasons(dateTime = dateTimeBottom, daylight = daylightBottom.formatDuration(), sunAngle = sunAngleBottom, color = color)
}