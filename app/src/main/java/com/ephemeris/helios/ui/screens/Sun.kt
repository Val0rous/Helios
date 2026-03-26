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
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.PathCard
import com.ephemeris.helios.ui.theme.MaterialColors
import com.ephemeris.helios.utils.Coordinates
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { PathCard(hours, getAngles(coordinates.latitude, 23.44)) }
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Sunrise & Sunset")
                    TextEntry(text1 = "10:30 AM", text2 = "@ 155.0°", icon1 = R.drawable.ic_wb_sunny_filled, desc1 = "Sunrise")
                    TextEntry(text1 = "10:00 PM", text2 = "@ 275.0°", icon1 = R.drawable.ic_wb_twilight_filled, desc1 = "Sunset")
                },
                rightCard = {
                    HeaderEntry(text = "Solar Noon")
                    TextEntry(text1 = "12:15 PM", text2 = "@ 180.1°", icon1 = R.drawable.ic_pace, desc1 = "Time of Solar Noon")
                    TextEntry(text1 = "69.2°", icon1 = R.drawable.ic_brightness_7, desc1 = "Altitude at Solar Noon")
                }
            )
        }
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Live Metrics")
                    TextEntry(text1 = "1,100", text2 = "W/m²", icon1 = R.drawable.ic_bolt_filled, desc1 = "Current Irradiance")
                    TextEntry(text1 = "UVI 10", text2 = "250 mW/m²", icon1 = R.drawable.ic_beach_access_filled, desc1 = "Current UV Index")
                    TextEntry(text1 = "90,000", text2 = "Lux", icon1 = R.drawable.ic_lightbulb_filled, desc1 = "Current Luminance")
                    TextEntry(text1 = "0.94 : 1", icon1 = R.drawable.ic_ev_shadow_filled, desc1 = "Current Shadow Ratio")
                },
                rightCard = {
                    HeaderEntry(text = "Daily Peaks")
                    TextEntry(text1 = "1,368", text2 = "W/m²", icon1 = R.drawable.ic_bolt, desc1 = "Max Irradiance")
                    TextEntry(text1 = "UVI 12", text2 = "300 mW/m²", icon1 = R.drawable.ic_beach_access, desc1 = "Max UV Index")
                    TextEntry(text1 = "120,000", text2 = "Lux", icon1 = R.drawable.ic_lightbulb, desc1 = "Max Luminance")
                    TextEntry(text1 = "0.38 : 1", icon1 = R.drawable.ic_ev_shadow, desc1 = "Min Shadow Ratio")
                }
            )
        }
        val goldenHourColor = MaterialColors.Amber500
        val blueHourColor = MaterialColors.Blue700
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Golden Hour", color = goldenHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = goldenHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = goldenHourColor)
                },
                rightCard = {
                    HeaderEntry(text = "Blue Hour", color = blueHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = blueHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = blueHourColor)
                }
            )
        }
        val pinkHourColor = MaterialColors.Pink500
        val alpenglowColor = MaterialColors.Red700
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Pink Hour", color = pinkHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = pinkHourColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = pinkHourColor)
                },
                rightCard = {
                    HeaderEntry(text = "Alpenglow", color = alpenglowColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = alpenglowColor)
                    TextEntryHours(text1 = "10:30 AM", text2 = "11:40 PM", text3 = "10h 10m", color = alpenglowColor)
                }
            )
        }

        val civilColor = MaterialColors.DeepPurple200
        val nauticalColor = MaterialColors.DeepPurple300
        val astroColor = MaterialColors.DeepPurple500
        val twilightColor = MaterialColors.DeepPurple500
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Dawn", color = twilightColor)
                    TextEntryHours(text1 = "Civil", text2 = "10:30 PM", text3 = "10h 10m", color = civilColor)
                    TextEntryHours(text1 = "Nautical", text2 = "11:00 PM", text3 = "10h 10m", color = nauticalColor)
                    TextEntryHours(text1 = "Astro", text2 = "11:30 PM", text3 = "10h 10m", color = astroColor)
                },
                rightCard = {
                    HeaderEntry(text = "Dusk", color = twilightColor)
                    TextEntryHours(text1 = "Civil", text2 = "10:30 PM", text3 = "10h 10m", color = civilColor)
                    TextEntryHours(text1 = "Nautical", text2 = "11:00 PM", text3 = "10h 10m", color = nauticalColor)
                    TextEntryHours(text1 = "Astro", text2 = "11:30 PM", text3 = "10h 10m", color = astroColor)
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
fun TextEntry(
    text1: String,
    text2: String = "",
    icon1: Int? = null,
    icon2: Int? = null,
    desc1: String = "",
    desc2: String = "",
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
        if (icon1 != null) Icon(painter = painterResource(id = icon1), contentDescription = desc1, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = text1, style = textStyle)
        if(text2 != "") Text(text = text2, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
fun TextEntryHours(
    text1: String,
    text2: String = "",
    text3: String = "",
    icon1: Int? = null,
    icon2: Int? = null,
    icon3: Int? = null,
    desc1: String = "",
    desc2: String = "",
    desc3: String = "",
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
            Row() {
                if (icon1 != null) Icon(painter = painterResource(id = icon1), contentDescription = desc1, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(text = text1, style = textStyle)
            }
            Row() {
                if (icon2 != null) Icon(painter = painterResource(id = icon2), contentDescription = desc2, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                if(text2 != "") Text(text = text2, style = textStyle)
            }
        }
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

        Row() {
            if (icon3 != null) Icon(painter = painterResource(id = icon3), contentDescription = desc3, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            if(text3 != "") Text(text = text3, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        }
    }
}