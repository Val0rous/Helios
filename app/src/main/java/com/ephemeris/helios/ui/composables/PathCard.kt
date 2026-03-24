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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
                item {
                    CustomFilterChip("Elevation", R.drawable.ic_sunny, R.drawable.ic_sunny_filled, {}, true) // Altitude
                }
                item {
                    CustomFilterChip("Irradiance", R.drawable.ic_bolt, R.drawable.ic_bolt_filled, {}) // Energy
                }
                item {
                    CustomFilterChip("Illuminance", R.drawable.ic_lightbulb, R.drawable.ic_lightbulb_filled, {}) // Lux
                }
                item {
                    CustomFilterChip("Path", R.drawable.ic_explore, R.drawable.ic_explore_filled, {}) // Trajectory
                }
                item {
                    CustomFilterChip("Shadows", R.drawable.ic_ev_shadow, R.drawable.ic_ev_shadow_filled, {})
                }
                item {
                    CustomFilterChip("Atmosphere", R.drawable.ic_foggy, R.drawable.ic_foggy_filled, {}) // AirMass
                }
                item {
                    CustomFilterChip("UV Intensity", R.drawable.ic_beach_access, R.drawable.ic_beach_access_filled, {}) // Intensity, UV
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
                    .padding(8.dp)
                    .padding(bottom = 2.dp),
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
internal fun CustomFilterChip(label: String, icon: Int, filledIcon: Int, onClick: () -> Unit, isSelected: Boolean = false) {
    FilterChip(
        selected = isSelected,
        onClick = { onClick },
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = (if (isSelected) filledIcon else icon)),
                contentDescription = "",
                modifier = Modifier.size(18.dp)
            )
        }
    )
}