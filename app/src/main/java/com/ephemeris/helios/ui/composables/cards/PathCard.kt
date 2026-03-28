package com.ephemeris.helios.ui.composables.cards

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ephemeris.helios.ui.composables.PathChart
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.SunChartTypes
import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.getSunPhase
import com.ephemeris.helios.utils.round

@Composable
fun PathCard(
    xValues: FloatArray,
    yValues: FloatArray,
    events: SolarEphemeris.DailyEvents,
    currentHour: Float = 15f,
    currentPosition: SolarEphemeris.SolarPosition,
) {
    var selectedChartType by remember { mutableStateOf(SunChartTypes.ELEVATION) }
    OutlinedCard(
        modifier = Modifier.Companion
            .fillMaxWidth()
//            .aspectRatio(2f)
            .padding(horizontal = 16.dp)
    ) {
        Column() {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
                modifier = Modifier.Companion.padding(horizontal = 8.dp)
            ) {
                items(SunChartTypes.entries) { type ->
                    ChartSelectorChip(
                        chartType = type,
                        isSelected = type == selectedChartType,
                        onSelectedChartTypeChange = { selectedChartType = it }
                    )
                }
            }
            PathChart(
                xValues = xValues,
                yValues = yValues,
                currentHour = currentHour,
                modifier = Modifier.Companion
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 0.dp)
            )
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 8.dp, bottom = 9.dp, start = 2.dp, end = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Companion.CenterVertically
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
        modifier = Modifier.Companion
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        thickness = Dp.Companion.Hairline,
        color = DividerDefaults.color
    )
}

@Composable
internal fun CustomColumn(header: String, value: String) {
    val textStyle = TextStyle(fontSize = (14).sp, fontFamily = FontFamily.Companion.Default)
    val verticalSpacing = 4.dp
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Text(
            text = header,
            style = textStyle.copy(
                fontWeight = FontWeight.Companion.SemiBold,
                letterSpacing = (0).sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(text = value, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface))
    }
}