package com.ephemeris.helios.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.PathCard
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
    time: LocalDateTime,
    isAutoUpdate: Boolean,
    coordinates: Coordinates,
    onTimeChange: (LocalDateTime) -> Unit,
    onAutoUpdateChange: (Boolean) -> Unit,
    onLocationChange: (Coordinates) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            val dayOfWeek = time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
            val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            val datePart = time.format(dateFormatter)
            val timePart = time.format(timeFormatter)
            val dateTime = "$dayOfWeek $datePart \t $timePart".replace(",", "").uppercase()
            Text(
                text = dateTime,
                modifier = Modifier.padding(16.dp),
                style = TextStyle(fontFamily = FontFamily.Monospace)
            )
        }
        item { PathCard(hours, getAngles(coordinates.latitude, 23.44)) }
        item {
            SmallCardRow(
                leftCard = {
                    HeaderEntry(text = "Sunrise & Sunset")
                    TextEntry(text1 = "10:30 AM", text2 = "@ 155.0°", icon = R.drawable.ic_wb_sunny_filled, desc = "Sunrise")
                    TextEntry(text1 = "10:00 PM", text2 = "@ 275.0°", icon = R.drawable.ic_wb_twilight_filled, desc = "Sunset")
                },
                rightCard = {
                    HeaderEntry(text = "Solar Noon")
                    TextEntry(text1 = "12:15 PM", text2 = "@ 180.1°", icon = R.drawable.ic_pace, desc = "Time of Solar Noon")
                    TextEntry(text1 = "69.2°", icon = R.drawable.ic_brightness_7, desc = "Altitude at Solar Noon")
                }
            )
        }
    }
}

@Composable
internal fun SmallCardRow(
    leftCard: @Composable () -> Unit,
    rightCard: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp)
    ) {
        // Left Card
        Card(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                leftCard()
            }
        }
        // Right Card
        Card(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rightCard()
            }
        }
    }
}

@Composable
fun HeaderEntry(text: String) {
    val headerStyle = TextStyle(fontSize = (13.5).sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    Text(text = text, style = headerStyle)
}

@Composable
fun TextEntry(text1: String, text2: String = "", icon: Int? = null, desc: String = "") {
    val textStyle = TextStyle(fontSize = (13.5).sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        if (icon != null) Icon(painter = painterResource(id = icon), contentDescription = desc, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = text1, style = textStyle)
        if(text2 != "") Text(text = text2, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}