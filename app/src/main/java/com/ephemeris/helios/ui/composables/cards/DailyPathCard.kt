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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.ChartSelectorChip
import com.ephemeris.helios.ui.composables.charts.DailyChart
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.MoonMetrics
import com.ephemeris.helios.utils.calc.SunMetrics
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import com.ephemeris.helios.utils.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.round

const val X_SIZE = 481 // 480 items

data class ChartArrays(
    val timeXValues: FloatArray,
    val xDataSets: Map<Charts, FloatArray>,
    val yDataSets: Map<Charts, FloatArray>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChartArrays

        if (!timeXValues.contentEquals(other.timeXValues)) return false

        // Deep compare the X maps
        if (xDataSets != other.xDataSets) return false
        for ((key, value) in xDataSets) {
            if (!value.contentEquals(other.xDataSets[key])) return false
        }

        // Deep compare the Y maps
        if (yDataSets != other.yDataSets) return false
        for ((key, value) in yDataSets) {
            if (!value.contentEquals(other.yDataSets[key])) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = timeXValues.contentHashCode()
        // Simple hash for the X map arrays
        xDataSets.forEach { (key, value) ->
            result = 31 * result + key.hashCode()
            result = 31 * result + value.contentHashCode()
        }
        // Simple hash for the Y map arrays
        yDataSets.forEach { (key, value) ->
            result = 31 * result + key.hashCode()
            result = 31 * result + value.contentHashCode()

        }
        return result
    }
}

@Composable
fun DailyPathCard(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    dayLength: Double,
    currentAltitude: Double,
    currentAzimuth: Double,
    phase: String = "",
    type: Charts,
    chartArrays: ChartArrays?
) {
    var selectedChartType by rememberSaveable { mutableStateOf(type) }

    val currentHour: Float by remember(currentTime) {
        derivedStateOf {
            // Always pass time (0-24). Never pass Azimuth!
            currentTime.hour.toFloat() + currentTime.minute / 60f + currentTime.second / 3600f
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
                val charts = when (type) {
                    is Charts.Sun -> Charts.Sun.Daily.entries
                    is Charts.Moon -> Charts.Moon.Daily.entries
                    else -> Charts.Moon.Daily.entries // Todo: Charts.Planets.Daily.entries, keeping Moon for now to be able to compile
                }
                items(charts) { type ->
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
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val isTrajectory = selectedChartType.javaClass.simpleName.contains("Trajectory")
//                val xValues = if (isTrajectory) arrays.xDataSets[selectedChartType] else arrays.timeXValues

                val yValues = when (selectedChartType) {
                    is Charts.SunMoonCombo.Daily.Elevation -> arrays.yDataSets[Charts.Sun.Daily.Elevation]
                    is Charts.SunMoonCombo.Daily.Trajectory -> arrays.yDataSets[Charts.Sun.Daily.Trajectory]
                    else -> arrays.yDataSets[selectedChartType]
                } ?: FloatArray(X_SIZE)

                val xValues = if (isTrajectory) {
                    arrays.xDataSets[selectedChartType] ?: FloatArray(X_SIZE)
                } else {
                    arrays.timeXValues
                }

                val cornerRadius = 12.dp
                DailyChart(
                    xValues = xValues,
                    yValues = yValues,
                    currentHour = currentHour,
                    chartType = selectedChartType,
                    currentAzimuth = currentAzimuth.toFloat(),
                    currentAltitude = currentAltitude.toFloat(),
                    modifier = Modifier
                        .aspectRatio(2f)
                        .clip(
                            RoundedCornerShape(
                                bottomStart = cornerRadius,
                                bottomEnd = cornerRadius
                            )
                        )
                        .padding(vertical = 0.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 8.dp, bottom = 9.dp, start = 2.dp, end = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daylengthText = when (type) {
                    is Charts.Sun -> R.string.day_length
                    is Charts.Moon -> R.string.duration
                    else -> R.string.uptime
                }
                CustomColumn(stringResource(daylengthText), dayLength.formatDuration(true))
                CustomVerticalDivider()
                CustomColumn(stringResource(R.string.altitude), "${currentAltitude.round()}°")
                CustomVerticalDivider()
                CustomColumn(stringResource(R.string.azimuth), "${currentAzimuth.round()}°")
                val phaseText = R.string.phase
                if (phase != "") {
                    CustomVerticalDivider()
                    CustomColumn(stringResource(phaseText), phase)
                }
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

internal fun generateSunData(
    localDate: LocalDate,
    hoursCalc: DoubleArray,
    coordinates: Coordinates,
    tzOffset: Double,
    dailyOzone: Double
): Pair<Map<Charts, FloatArray>, Map<Charts, FloatArray>> {
    val elevationCalc = DoubleArray(X_SIZE)
    val azimuthsCalc = FloatArray(X_SIZE)
    val xMap = mutableMapOf<Charts, FloatArray>()
    val yMap = mutableMapOf<Charts, FloatArray>()
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
        ozoneDU = dailyOzone,
        outIrradiance = irradiance,
        outUvi = uvIntensity,
        outIlluminance = illuminance,
        outShadowRatio = shadowRatio,
        outColorTemp = colorTemp,
        outAirMass = airMass
    )

    // Map the arrays to their specific Sun enum keys
    yMap[Charts.Sun.Daily.Elevation] = elevation
    yMap[Charts.Sun.Daily.Irradiance] = irradiance
    yMap[Charts.Sun.Daily.UvIntensity] = uvIntensity
    yMap[Charts.Sun.Daily.Trajectory] = elevation
    yMap[Charts.Sun.Daily.Illuminance] = illuminance
    yMap[Charts.Sun.Daily.Shadows] = shadowRatio
    yMap[Charts.Sun.Daily.ColorTemperature] = colorTemp
    yMap[Charts.Sun.Daily.AirMass] = airMass

    xMap[Charts.Sun.Daily.Trajectory] = azimuthsCalc

    return xMap to yMap
}

internal fun generateMoonData(
    localDate: LocalDate,
    hoursCalc: DoubleArray,
    coordinates: Coordinates,
    tzOffset: Double,
    currentTime: ZonedDateTime
): Pair<Map<Charts, FloatArray>, Map<Charts, FloatArray>> {
    val elevationCalc = DoubleArray(X_SIZE)
    val azimuthsCalc = FloatArray(X_SIZE)
    val xMap = mutableMapOf<Charts, FloatArray>()
    val yMap = mutableMapOf<Charts, FloatArray>()
    for (i in 0 until X_SIZE) {
        val position = LunarEphemeris.getPositionAtHour(
            date = localDate,
            decimalHour = hoursCalc[i],
            coordinates = coordinates,
            tzOffsetHours = tzOffset
        )
        elevationCalc[i] = position.altitude
        azimuthsCalc[i] = position.azimuth.toFloat()
    }
    val elevation = FloatArray(X_SIZE) { elevationCalc[it].toFloat() }
    val illuminance = FloatArray(X_SIZE)
    val shadowRatio = FloatArray(X_SIZE)
    val colorTemp = FloatArray(X_SIZE)
    val airMass = FloatArray(X_SIZE)

    val res = MoonMetrics.calculateMetrics(
        time = currentTime,
        coordinates = coordinates
    )

    yMap[Charts.Moon.Daily.Elevation] = elevation
    yMap[Charts.Moon.Daily.Trajectory] = elevation
//                    yDataMap[Charts.Moon.Daily.Illuminance] = res.illuminanceLux

    xMap[Charts.Moon.Daily.Trajectory] = azimuthsCalc
    // Todo: fix and add Moon charts
    return xMap to yMap
}