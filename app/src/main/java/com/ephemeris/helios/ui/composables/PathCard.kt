package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ephemeris.helios.R

@Composable
fun PathCard(
    xValues: FloatArray,
    yValues: FloatArray,
    currentHour: Float = 15f
) {
    var selectedChartType by remember { mutableStateOf(SunChartTypes.ELEVATION) }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
//            .aspectRatio(2f)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column() {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                items(SunChartTypes.entries) { type ->
                    CustomFilterChip(
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
                modifier = Modifier
                    .aspectRatio(2f)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 0.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = 8.dp, bottom = 9.dp, start = 2.dp, end = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomColumn("Day Length", "12h 30m 45s")
                CustomVerticalDivider()
                CustomColumn("Altitude", "47.0°")
                CustomVerticalDivider()
                CustomColumn("Azimuth", "185.0°")
                CustomVerticalDivider()
                CustomColumn("Shadow Ratio", "0.94 : 1")
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
    val textStyle = TextStyle(fontSize = (13.5).sp, fontFamily = FontFamily.Monospace)
    val verticalSpacing = 3.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        Text(text = header, style = textStyle.copy(fontWeight = FontWeight.Bold, letterSpacing = (0.3).sp, color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = value, style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface))
    }
}

@Composable
internal fun CustomFilterChip(chartType: SunChartTypes, isSelected: Boolean, onSelectedChartTypeChange: (SunChartTypes) -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = { onSelectedChartTypeChange(chartType) },
        label = { Text(text = chartType.label) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = (if (isSelected) chartType.filledIcon else chartType.icon)),
                contentDescription = "",
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

internal enum class SunChartTypes(val label: String, val icon: Int, val filledIcon: Int) {
    ELEVATION("Elevation", R.drawable.ic_sunny, R.drawable.ic_sunny_filled), // Altitude
    IRRADIANCE("Irradiance", R.drawable.ic_bolt, R.drawable.ic_bolt_filled), // Energy
    ILLUMINANCE("Illuminance", R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled), // Lux
    TRAJECTORY("Trajectory", R.drawable.ic_explore, R.drawable.ic_explore_filled), // Path
    SHADOWS("Shadows", R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled),
    AIR_MASS("Air Mass", R.drawable.ic_foggy, R.drawable.ic_foggy_filled), // Atmosphere
    UV_INTENSITY("UV Intensity", R.drawable.ic_beach_access, R.drawable.ic_beach_access_filled) // Intensity, UV
}