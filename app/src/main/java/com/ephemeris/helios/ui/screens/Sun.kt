package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.PathCard
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.formatNumber
import com.ephemeris.helios.utils.round
import com.ephemeris.helios.utils.roundToSignificant
import com.ephemeris.helios.utils.timeFormat
import kotlin.collections.toFloatArray
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
    coordinates: Coordinates,
    onLocationChange: (Coordinates) -> Unit,
    events: SolarEphemeris.DailyEvents
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { PathCard(hours, getAngles(coordinates.latitude, 23.44)) }
        item {
            SmallCardRow(
                leftCard = {
                    SunriseSunsetEntry(
                        sunriseTime = SolarEphemeris.formatDecimalHours(events.sunrise),
                        sunsetTime = SolarEphemeris.formatDecimalHours(events.sunset),
                        sunriseAzimuth = 155.0,
                        sunsetAzimuth = 275.0
                    )
                },
                rightCard = {
                    SolarNoonEntry(
                        noonTime = SolarEphemeris.formatDecimalHours(events.solarNoon),
                        noonAzimuth = 180.1,
                        noonAltitude = 69.2
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
                    HeaderEntry(text = stringResource(R.string.golden_hour), color = goldenHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = goldenHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = goldenHourColor)
                },
                rightCard = {
                    HeaderEntry(text = stringResource(R.string.blue_hour), color = blueHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = blueHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = blueHourColor)
                }
            )
        }
        val pinkHourColor = MaterialColors.Pink500
        val alpenglowColor = MaterialColors.Red700
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = stringResource(R.string.pink_hour), color = pinkHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = pinkHourColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = pinkHourColor)
                },
                rightCard = {
                    HeaderEntry(text = stringResource(R.string.alpenglow), color = alpenglowColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = alpenglowColor)
                    TextEntryHours(label = "10:30 AM", time = "11:40 PM", duration = "10h 10m", color = alpenglowColor)
                }
            )
        }

        val civilColor = MaterialColors.DeepPurple200
        val nauticalColor = MaterialColors.DeepPurple300
        val astroColor = MaterialColors.DeepPurple500
        val twilightColor = MaterialColors.DeepPurple500
        val nightColor = MaterialColors.DeepPurple900
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = stringResource(R.string.dawn), color = twilightColor)
                    TextEntryHours(label = stringResource(R.string.civil), time = "10:30 PM", duration = "10h 10m", color = civilColor)
                    TextEntryHours(label = stringResource(R.string.nautical), time = "11:00 PM", duration = "10h 10m", color = nauticalColor)
                    TextEntryHours(label = stringResource(R.string.astro), time = "11:30 PM", duration = "10h 10m", color = astroColor)
                    TextEntryHours(label = stringResource(R.string.night), time = "12:30 AM", duration = "4h 30m", color = nightColor)
                },
                rightCard = {
                    HeaderEntry(text = stringResource(R.string.dusk), color = twilightColor)
                    TextEntryHours(label = stringResource(R.string.civil), time = "10:30 PM", duration = "10h 10m", color = civilColor)
                    TextEntryHours(label = stringResource(R.string.nautical), time = "11:00 PM", duration = "10h 10m", color = nauticalColor)
                    TextEntryHours(label = stringResource(R.string.astro), time = "11:30 PM", duration = "10h 10m", color = astroColor)
                    TextEntryHours(label = stringResource(R.string.night), time = stringResource(R.string.not_for_this_day), duration = "", color = nightColor)
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
internal fun SmallCard(card: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val paddingValue = if (modifier == Modifier) 16.dp else 8.dp
    OutlinedCard(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = paddingValue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            card()
        }
    }
}

@Composable
internal fun SmallCardRow(
    leftCard: @Composable () -> Unit,
    rightCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp)
    ) {
        // Left Card
        SmallCard(
            card = { leftCard() },
            modifier = Modifier.weight(1f)
        )
        // Right Card
        SmallCard(
            card = { rightCard() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun HeaderEntry(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val headerStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, color = color)
    Text(text = text, style = headerStyle)
}

@Composable
fun TextVariant(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val textStyle = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.Default, color = color)
    Text(text = text, style = textStyle)
}

@Composable
fun TextEntry(
    text: String,
    textVariant: String = "",
    icon: Int? = null,
    iconVariant: Int? = null,
    desc: String = "",
    descVariant: String = "",
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default, color = color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        if (icon != null) Icon(painter = painterResource(id = icon), contentDescription = desc, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = text, style = textStyle)
        if(textVariant != "") TextVariant(textVariant)
    }
}

@Composable
fun TextEntryHours(
    label: String,
    time: String = "",
    duration: String = "",
    color: Color = DividerDefaults.color
) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default, color = MaterialTheme.colorScheme.onSurface)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            thickness = (1.5).dp,
            color = color
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = textStyle)
            if (time != "") Text(text = time, style = textStyle)
        }
        if (duration != "") {
            val color = DividerDefaults.color
            // The Curly Brace Separator
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp) // Space for the brace
                    .padding(vertical = 4.dp)
            ) {
                val strokeWidth = 1.dp.toPx()
                val w = size.width
                val h = size.height
                val r = 4.dp.toPx() // Curvature radius

                val path = androidx.compose.ui.graphics.Path().apply {
                    // Top curve
                    moveTo(0f, 0f)
                    quadraticTo(w * 0.5f, 0f, w * 0.5f, r)
                    // Top vertical line
                    lineTo(w * 0.5f, h * 0.5f - r)
                    // Middle point (the tip of the brace)
                    quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
                    quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h * 0.5f + r)
                    // Bottom vertical line
                    lineTo(w * 0.5f, h - r)
                    // Bottom curve
                    quadraticTo(w * 0.5f, h, 0f, h)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
            TextVariant(duration)
        }
    }
}

@Composable
fun SunriseSunsetEntry(
    sunriseTime: String = "",
    sunsetTime: String = "",
    sunriseAzimuth: Double = 0.0,
    sunsetAzimuth: Double = 0.0
) {
    HeaderEntry(text = stringResource(R.string.sunrise_sunset))
    // Todo: implement always above/always below behavior
    TextEntry(text = sunriseTime, textVariant = "@ $sunriseAzimuth°", icon = R.drawable.ic_wb_sunny_filled, desc = "Sunrise")
    TextEntry(text = sunsetTime, textVariant = "@ $sunsetAzimuth°", icon = R.drawable.ic_wb_twilight_filled, desc = "Sunset")
}

@Composable
fun SolarNoonEntry(
    noonTime: String = "",
    noonAzimuth: Double,
    noonAltitude: Double
) {
    // Todo: make altitude turn red if sun is always below horizon
    HeaderEntry(text = stringResource(R.string.solar_noon))
    TextEntry(text = noonTime, textVariant = "@ $noonAzimuth°", icon = R.drawable.ic_pace, desc = "Time of Solar Noon")
    TextEntry(text = "$noonAltitude°", textVariant = "", icon = R.drawable.ic_brightness_7, desc = "Altitude at Solar Noon")
}

@Composable
fun LiveMetricsEntry(
    irradiance: Double = 0.0,
    uvIntensity: Double = 0.0,
    luminance: Double = 0.0,
    shadowRatio: Double = 0.0
) {
    HeaderEntry(text = stringResource(R.string.live_metrics))
    TextEntry(text = "${irradiance.roundToSignificant()}", textVariant = "W/m²", icon = R.drawable.ic_bolt_filled, desc = "Current Irradiance")
    TextEntry(text = "UVI ${uvIntensity.roundToSignificant(2)}", textVariant = "250 mW/m²", icon = R.drawable.ic_beach_access_filled, desc = "Current UV Index")
    TextEntry(text = formatNumber(luminance.roundToSignificant()), textVariant = "Lux", icon = R.drawable.ic_lightbulb_filled, desc = "Current Luminance")
    TextEntry(text = "${shadowRatio.roundToSignificant()} : 1", icon = R.drawable.ic_ev_shadow_filled, desc = "Current Shadow Ratio")
}

@Composable
fun DailyPeaksEntry(
    irradiance: Double = 0.0,
    uvIntensity: Double = 0.0,
    luminance: Double = 0.0,
    shadowRatio: Double = 0.0
) {
    HeaderEntry(text = stringResource(R.string.daily_peaks))
    TextEntry(text = "${irradiance.roundToSignificant()}", textVariant = "W/m²", icon = R.drawable.ic_bolt, desc = "Max Irradiance")
    TextEntry(text = "UVI ${uvIntensity.roundToSignificant()}", textVariant = "300 mW/m²", icon = R.drawable.ic_beach_access, desc = "Max UV Index")
    TextEntry(text = formatNumber(luminance.roundToSignificant()), textVariant = "Lux", icon = R.drawable.ic_lightbulb, desc = "Max Luminance")
    TextEntry(text = "${shadowRatio.roundToSignificant()} : 1", icon = R.drawable.ic_ev_shadow, desc = "Min Shadow Ratio")
}
