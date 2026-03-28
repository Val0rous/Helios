package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.PathCard
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.entries.DailyPeaksEntry
import com.ephemeris.helios.ui.composables.entries.DurationEntry
import com.ephemeris.helios.ui.composables.entries.HeaderEntry
import com.ephemeris.helios.ui.composables.entries.LiveMetricsEntry
import com.ephemeris.helios.ui.composables.entries.NightEntry
import com.ephemeris.helios.ui.composables.entries.PlutoTimeEntry
import com.ephemeris.helios.ui.composables.entries.SolarNoonEntry
import com.ephemeris.helios.ui.composables.entries.SunriseSunsetEntry
import com.ephemeris.helios.ui.composables.entries.TextEntryHours
import com.ephemeris.helios.ui.composables.entries.TwilightEntry
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.round
import com.ephemeris.helios.utils.roundToSignificant
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

val hours = FloatArray(481) { round(it * 5f) / 100f }
val angles = DoubleArray(hours.size) { Math.toDegrees(asin(cos(Math.toRadians(15.0)*(hours[it] - 12f)))) }

fun toSin(list: FloatArray): FloatArray {
    return list.map { 90f * sin(Math.toRadians(it.toDouble())).toFloat() }.toFloatArray()
}

fun toCos(list: FloatArray): FloatArray {
    return list.map { 90f * cos(Math.toRadians(90.0 - it.toDouble())).toFloat() }.toFloatArray()
}

fun getAngles(lat: Double, dec: Double, toSin: Boolean = false, toCos: Boolean = false): FloatArray {
    val list = FloatArray(angles.size) { (dec + (90.0 - lat) * sin(Math.toRadians(angles[it]))).toFloat() }
    if (toSin) return toSin(list)
    if (toCos) return toCos(list)
    return list
}

@Composable
fun Sun(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    currentPosition: SolarEphemeris.SolarPosition,
    events: SolarEphemeris.DailyEvents,
    durations: SolarEphemeris.DailyDurations
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            PathCard(
                xValues = hours,
                yValues = getAngles(coordinates.latitude, 23.44),
                events = events,
                currentHour = currentTime.hour.toFloat() + currentTime.minute.toFloat() / 60f,
                currentPosition = currentPosition
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    SunriseSunsetEntry(
                        sunriseTime = events.sunrise.formatDecimalHours(),
                        sunsetTime = events.sunset.formatDecimalHours(),
                        sunriseAzimuth = events.sunriseAzimuth!!.round(),
                        sunsetAzimuth = events.sunsetAzimuth!!.round()
                    )
                },
                rightCard = {
                    SolarNoonEntry(
                        noonTime = events.solarNoon.formatDecimalHours(),
                        noonAzimuth = events.solarNoonAzimuth.round(),
                        noonAltitude = events.solarNoonAltitude.round()
                    )
                }
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    LiveMetricsEntry(
                        irradiance = 1100.0,
                        uvIntensity = 10.0,
                        luminance = 90000.0,
                        shadowRatio = 0.94
                    )
                },
                rightCard = {
                    DailyPeaksEntry(
                        irradiance = 1368.0,
                        uvIntensity = 12.0,
                        luminance = 120000.0,
                        shadowRatio = 0.38
                    )
                }
            )
        }
        val goldenHourColor = MaterialColors.Amber500
        val blueHourColor = MaterialColors.Blue700
        item {
            SmallCardRow(
                leftCard = {
                    DurationEntry(
                        title = R.string.golden_hour,
                        color = goldenHourColor,
                        morningStartTime = events.dawnGoldenLower.formatDecimalHours(),
                        morningEndTime = events.dawnGoldenUpper.formatDecimalHours(),
                        morningDuration = durations.goldenHour.morning.formatDuration(),
                        eveningStartTime = events.duskGoldenUpper.formatDecimalHours(),
                        eveningEndTime = events.duskGoldenLower.formatDecimalHours(),
                        eveningDuration = durations.goldenHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.blue_hour,
                        color = blueHourColor,
                        morningStartTime = events.dawnBlueLower.formatDecimalHours(),
                        morningEndTime = events.dawnBlueUpper.formatDecimalHours(),
                        morningDuration = durations.blueHour.morning.formatDuration(),
                        eveningStartTime = events.duskBlueUpper.formatDecimalHours(),
                        eveningEndTime = events.duskBlueLower.formatDecimalHours(),
                        eveningDuration = durations.blueHour.evening.formatDuration()
                    )
                }
            )
        }
        val pinkHourColor = MaterialColors.Pink500
        val alpenglowColor = MaterialColors.Red700
        item {
            SmallCardRow(
                leftCard = {
                    DurationEntry(
                        title = R.string.pink_hour,
                        color = pinkHourColor,
                        morningStartTime = events.dawnPinkLower.formatDecimalHours(),
                        morningEndTime = events.dawnPinkUpper.formatDecimalHours(),
                        morningDuration = durations.pinkHour.morning.formatDuration(),
                        eveningStartTime = events.duskPinkUpper.formatDecimalHours(),
                        eveningEndTime = events.duskPinkLower.formatDecimalHours(),
                        eveningDuration = durations.pinkHour.evening.formatDuration()
                    )
                },
                rightCard = {
                    DurationEntry(
                        title = R.string.alpenglow,
                        color = alpenglowColor,
                        morningStartTime = events.dawnAlpenglowLower.formatDecimalHours(),
                        morningEndTime = events.dawnAlpenglowUpper.formatDecimalHours(),
                        morningDuration = durations.alpenglow.morning.formatDuration(),
                        eveningStartTime = events.duskAlpenglowUpper.formatDecimalHours(),
                        eveningEndTime = events.duskAlpenglowLower.formatDecimalHours(),
                        eveningDuration = durations.alpenglow.evening.formatDuration()
                    )
                }
            )
        }

        item {
            SmallCardRow(
                leftCard = {
                    TwilightEntry(
                        title = R.string.dawn,
                        subtitle = R.string.start,
                        civilTime = events.dawnCivil.formatDecimalHours(),
                        civilDuration = durations.civilTwilight.morning.formatDuration(),
                        nauticalTime = events.dawnNautical.formatDecimalHours(),
                        nauticalDuration = durations.nauticalTwilight.morning.formatDuration(),
                        astroTime = events.dawnAstronomical.formatDecimalHours(),
                        astroDuration = durations.astronomicalTwilight.morning.formatDuration(),
                        nightTime = "",
                        nightDuration = ""
                    )
                },
                rightCard = {
                    TwilightEntry(
                        title = R.string.dusk,
                        subtitle = R.string.end,
                        civilTime = events.duskCivil.formatDecimalHours(),
                        civilDuration = durations.civilTwilight.evening.formatDuration(),
                        nauticalTime = events.duskNautical.formatDecimalHours(),
                        nauticalDuration = durations.nauticalTwilight.evening.formatDuration(),
                        astroTime = events.duskAstronomical.formatDecimalHours(),
                        astroDuration = durations.astronomicalTwilight.evening.formatDuration(),
                        nightTime = "",
                        nightDuration = ""
                    )
                }
            )
        }

        item {
            SmallCardRow(
                leftCard = {
                    NightEntry(
                        duration = durations.night.total.formatDuration(true),
                        midnightTime = events.solarMidnight.formatDecimalHours(),
                        midnightAzimuth = events.solarMidnightAzimuth.round(),
                        midnightAltitude = events.solarMidnightAltitude.round()
                    )
                },
                rightCard = {
                    PlutoTimeEntry(
                        morningTime = events.morningPlutoTime.formatDecimalHours(),
                        eveningTime = events.eveningPlutoTime.formatDecimalHours()
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}
