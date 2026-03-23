package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    }
}