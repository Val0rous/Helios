package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.MoonPhaseAppearance
import com.ephemeris.helios.ui.composables.charts.MoonPhaseIcon
import com.ephemeris.helios.ui.composables.charts.rememberChartIconDrawer
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatDecimalHours
import com.ephemeris.helios.utils.round

@Composable
fun InfoPager(
    currentSunPosition: SolarEphemeris.SolarPosition,
    sunEvents: SolarEphemeris.DailyEvents,
    currentMoonPosition: LunarEphemeris.LunarPosition,
    moonEvents: LunarEphemeris.LunarDailyEvents,
    moonPhase: MoonPhaseAppearance = MoonPhaseAppearance.Full,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        val textStyle = MaterialTheme.typography.bodyMedium
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            HorizontalPager(
                state = pagerState,
            ) { page ->
                when (page) {
                    0 -> {
                        // Sun
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                        ) {
                            val sunIconDrawer = rememberChartIconDrawer(Charts.Sun.Daily.Elevation)
                            Column(
                                modifier = Modifier.width(64.dp).fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .drawBehind {
                                            sunIconDrawer(size.width, false)
                                        }
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.sun),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${currentSunPosition.altitude.round()}°",
                                    style = textStyle)
                                Text(
                                    text = "@ ${currentSunPosition.azimuth.round()}°",
                                    style = textStyle
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.sunrise),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = sunEvents.sunrise.formatDecimalHours(),
                                    style = textStyle
                                )
                                Text(
                                    text = "@ ${sunEvents.sunriseAzimuth.round()}°",
                                    style = textStyle
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.sunset),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = sunEvents.sunset.formatDecimalHours(),
                                    style = textStyle
                                )
                                Text(
                                    text = "@ ${sunEvents.sunsetAzimuth.round()}°",
                                    style = textStyle
                                )
                            }
                        }
                    }
                    1 -> {
                        // Moon
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                        ) {
                            Column(
                                modifier = Modifier.width(64.dp).fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                MoonPhaseIcon(
                                    phase = moonPhase,
                                    size = 36.dp,
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.moon),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${currentMoonPosition.altitude.round()}°",
                                    style = textStyle
                                )
                                Text(
                                    text = "@ ${currentMoonPosition.azimuth.round()}°",
                                    style = textStyle
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.moonrise),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = moonEvents.moonrise.formatDecimalHours(),
                                    style = textStyle
                                )
                                Text(
                                    text = "@ ${moonEvents.moonriseAzimuth?.round()}°",
                                    style = textStyle
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.moonset),
                                    fontWeight = FontWeight.SemiBold,
                                    style = textStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = moonEvents.moonset.formatDecimalHours(),
                                    style = textStyle
                                )
                                Text(
                                    text = "@ ${moonEvents.moonsetAzimuth?.round()}°",
                                    style = textStyle
                                )
                            }
                        }
                    }
                }
            }
            DotsIndicator(
                totalDots = 2,
                selectedIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}
