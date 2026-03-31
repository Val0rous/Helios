package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.ui.composables.ChartSelectorChip
import com.ephemeris.helios.ui.composables.charts.DailyAzimuthChart
import com.ephemeris.helios.ui.composables.charts.DailyTimeChart
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.SunMetrics
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import kotlin.math.round

const val X_SIZE = 481 // 480 items

data class ChartArrays(
    val hours: FloatArray,
    val elevation: FloatArray,
    val azimuths: FloatArray,
    val irradiance: FloatArray,
    val uvIntensity: FloatArray,
    val illuminance: FloatArray,
    val shadowRatio: FloatArray,
    val colorTemp: FloatArray,
    val airMass: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartArrays

        if (!hours.contentEquals(other.hours)) return false
        if (!elevation.contentEquals(other.elevation)) return false
        if (!azimuths.contentEquals(other.azimuths)) return false
        if (!irradiance.contentEquals(other.irradiance)) return false
        if (!uvIntensity.contentEquals(other.uvIntensity)) return false
        if (!illuminance.contentEquals(other.illuminance)) return false
        if (!shadowRatio.contentEquals(other.shadowRatio)) return false
        if (!colorTemp.contentEquals(other.colorTemp)) return false
        if (!airMass.contentEquals(other.airMass)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hours.contentHashCode()
        result = 31 * result + elevation.contentHashCode()
        result = 31 * result + azimuths.contentHashCode()
        result = 31 * result + irradiance.contentHashCode()
        result = 31 * result + uvIntensity.contentHashCode()
        result = 31 * result + illuminance.contentHashCode()
        result = 31 * result + shadowRatio.contentHashCode()
        result = 31 * result + colorTemp.contentHashCode()
        result = 31 * result + airMass.contentHashCode()
        return result
    }
}

@Composable
fun PathCard(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    events: SolarEphemeris.DailyEvents,
    currentPosition: SolarEphemeris.SolarPosition,
) {
    var selectedChartType by rememberSaveable { mutableStateOf<Charts>(Charts.Sun.Daily.Elevation) }
    var chartArrays by remember { mutableStateOf<ChartArrays?>(null) }

    val currentHour: Float by remember(currentTime) {
        derivedStateOf {
            // Always pass time (0-24). Never pass Azimuth!
            currentTime.hour.toFloat() + currentTime.minute / 60f + currentTime.second / 3600f
        }
    }

    // Isolate the 480-iteration math loop.
    // It will ONLY run when the Date or Coordinate change, ignoring the 12-second time tick.
    LaunchedEffect(currentTime.toLocalDate(), coordinates) {
        withContext(Dispatchers.Default) {
            val hoursCalc = DoubleArray(X_SIZE) { round(it * 5.0) / 100.0 }
            val elevationCalc = DoubleArray(hoursCalc.size)
            val azimuthsCalc = FloatArray(X_SIZE)

            val tzOffset = currentTime.offset.totalSeconds / 3600.0
            val localDate = currentTime.toLocalDate()

            for (i in 0 until X_SIZE) {
                val position = SolarEphemeris.getPositionAtHour(
                    date = localDate,
                    decimalHour = hoursCalc[i],
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude,
                    tzOffsetHours = tzOffset
                )
                elevationCalc[i] = position.altitude
                azimuthsCalc[i] = position.azimuth.toFloat()
            }

            val hours = FloatArray(X_SIZE) { hoursCalc[it].toFloat() }
            val elevation = FloatArray(X_SIZE) { elevationCalc[it].toFloat() }
            val irradiance = FloatArray(X_SIZE)
            val uvIntensity = FloatArray(X_SIZE)
            val illuminance = FloatArray(X_SIZE)
            val shadowRatio = FloatArray(X_SIZE)
            val colorTemp = FloatArray(X_SIZE)
            val airMass = FloatArray(X_SIZE)

            SunMetrics.calculateMetrics(
                sunElevationsDeg = elevationCalc,
                observerAltitudeMeters = coordinates.altitude,
                outIrradiance = irradiance,
                outUvi = uvIntensity,
                outIlluminance = illuminance,
                outShadowRatio = shadowRatio,
                outColorTemp = colorTemp,
                outAirMass = airMass
            )

            chartArrays = ChartArrays(
                hours, elevation, azimuthsCalc, irradiance, uvIntensity,
                illuminance, shadowRatio, colorTemp, airMass
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
//            .aspectRatio(2f)
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column() {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                items(Charts.Sun.Daily.entries) { type ->
                    ChartSelectorChip(
                        chartType = type,
                        isSelected = type == selectedChartType,
                        onSelectedChartTypeChange = { selectedChartType = it }
                    )
                }
            }
            // Show a placeholder or empty box while arrays are generating in the background
            val arrays = chartArrays
            if (arrays == null) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val xValues = when (selectedChartType) {
                    Charts.Sun.Daily.Trajectory -> arrays.azimuths
                    else -> arrays.hours
                }
                val yValues = when (selectedChartType) {
                    Charts.Sun.Daily.Elevation, Charts.Sun.Daily.Trajectory -> arrays.elevation
                    Charts.Sun.Daily.Irradiance -> arrays.irradiance
                    Charts.Sun.Daily.UvIntensity -> arrays.uvIntensity
                    Charts.Sun.Daily.Illuminance -> arrays.illuminance
                    Charts.Sun.Daily.Shadows -> arrays.shadowRatio
                    Charts.Sun.Daily.ColorTemperature -> arrays.colorTemp
                    Charts.Sun.Daily.AirMass -> arrays.airMass
                    else -> FloatArray(X_SIZE)
                }

                val cornerRadius = 12.dp

                when (selectedChartType) {
                    Charts.Sun.Daily.Trajectory -> DailyAzimuthChart(
                        xValues = xValues,
                        yValues = yValues,
                        currentHour = currentHour,
                        chartType = selectedChartType,
                        currentAzimuth = currentPosition.azimuth.toFloat(),
                        currentAltitude = currentPosition.altitude.toFloat(),
                        modifier = Modifier
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius))
                            .padding(vertical = 0.dp)
                    )

                    else -> DailyTimeChart(
                        xValues = xValues,
                        yValues = yValues,
                        currentHour = currentHour,
                        chartType = selectedChartType,
                        modifier = Modifier
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius))
                            .padding(vertical = 0.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 8.dp, bottom = 9.dp, start = 2.dp, end = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomColumn("Day Length", events.dayLength.formatDuration(true))
                CustomVerticalDivider()
                CustomColumn("Altitude", "${currentPosition.altitude.round()}°")
                CustomVerticalDivider()
                CustomColumn("Azimuth", "${currentPosition.azimuth.round()}°")
                CustomVerticalDivider()
                CustomColumn("Phase", getSunPhase(currentPosition.altitude).desc)
            }
        }
    }
}

@Composable
internal fun CustomVerticalDivider() {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        thickness = Dp.Hairline,
        color = DividerDefaults.color
    )
}

@Composable
internal fun CustomColumn(header: String, value: String) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Default)
    val verticalSpacing = 4.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Text(
            text = header,
            style = textStyle.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (0).sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(text = value, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface))
    }
}