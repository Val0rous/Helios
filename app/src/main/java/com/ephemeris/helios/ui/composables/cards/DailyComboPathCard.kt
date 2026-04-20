package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.ChartSelectorChip
import com.ephemeris.helios.ui.composables.charts.DailyChart
import com.ephemeris.helios.ui.composables.charts.DailyComboChart
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import com.ephemeris.helios.utils.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import kotlin.math.round

@Composable
fun DailyComboPathCard(
    currentTime: ZonedDateTime,
    coordinates: Coordinates,
    dayLength: Double,
    currentSunAltitude: Double,
    currentSunAzimuth: Double,
    currentMoonAltitude: Double,
    currentMoonAzimuth: Double,
    phase: String = "",
    type: Charts,
    sunChartArrays: ChartArrays?,
    moonChartArrays: ChartArrays?
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
                    is Charts.SunMoonCombo -> Charts.SunMoonCombo.Daily.entries
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
            if (sunChartArrays == null || moonChartArrays == null) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val isTrajectory = selectedChartType.javaClass.simpleName.contains("Trajectory")
//                val xValues = if (isTrajectory) arrays.xDataSets[selectedChartType] else arrays.timeXValues

                val primaryXValues = if (isTrajectory) {
                    sunChartArrays.xDataSets[Charts.Sun.Daily.Trajectory] ?: FloatArray(X_SIZE)
                } else {
                    sunChartArrays.timeXValues
                }

                val secondaryXValues = if (isTrajectory) {
                    moonChartArrays.xDataSets[Charts.Moon.Daily.Trajectory] ?: FloatArray(X_SIZE)
                } else {
                    moonChartArrays.timeXValues
                }

                val primaryYValues = when (selectedChartType) {
                    is Charts.SunMoonCombo.Daily.Elevation -> sunChartArrays.yDataSets[Charts.Sun.Daily.Elevation]
                    is Charts.SunMoonCombo.Daily.Trajectory -> sunChartArrays.yDataSets[Charts.Sun.Daily.Trajectory]
                    else -> sunChartArrays.yDataSets[selectedChartType]
                } ?: FloatArray(X_SIZE)

                val secondaryYValues = when (selectedChartType) {
                    is Charts.SunMoonCombo.Daily.Elevation -> moonChartArrays.yDataSets[Charts.Moon.Daily.Elevation]
                    is Charts.SunMoonCombo.Daily.Trajectory -> moonChartArrays.yDataSets[Charts.Moon.Daily.Trajectory]
                    else -> floatArrayOf()
                } ?: FloatArray(X_SIZE)

                val cornerRadius = 12.dp
                DailyComboChart(
                    sunXValues = primaryXValues,
                    sunYValues = primaryYValues,
                    moonXValues = secondaryXValues,
                    moonYValues = secondaryYValues,
                    currentHour = currentHour,
                    chartType = selectedChartType,
                    currentSunAzimuth = currentSunAzimuth.toFloat(),
                    currentSunAltitude = currentSunAltitude.toFloat(),
                    currentMoonAzimuth = currentMoonAzimuth.toFloat(),
                    currentMoonAltitude = currentMoonAltitude.toFloat(),
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
                CustomColumn(stringResource(R.string.altitude), "${currentSunAltitude.round()}°")
                CustomVerticalDivider()
                CustomColumn(stringResource(R.string.azimuth), "${currentSunAzimuth.round()}°")
                val phaseText = R.string.phase
                if (phase != "") {
                    CustomVerticalDivider()
                    CustomColumn(stringResource(phaseText), phase)
                }
            }
        }
    }
}