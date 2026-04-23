package com.ephemeris.helios.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.charts.rememberChartIconDrawer
import com.ephemeris.helios.utils.Charts
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatDecimalHours

@Composable
fun InfoPager(
    currentSunPosition: SolarEphemeris.SolarPosition,
    sunEvents: SolarEphemeris.DailyEvents,
    currentMoonPosition: LunarEphemeris.LunarPosition,
    moonEvents: LunarEphemeris.LunarDailyEvents,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        val pagerState = rememberPagerState(pageCount = { 2 })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            HorizontalPager(
                state = pagerState,
            ) { page ->
                when (page) {
                    0 -> {
                        // Sun
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sunIconDrawer = rememberChartIconDrawer(Charts.Sun.Daily.Elevation)
                            Column(
                                modifier = Modifier.width(64.dp)
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
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("${currentSunPosition.altitude}°")
                                Text("@${currentSunPosition.azimuth}°")
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.sunrise),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(sunEvents.sunrise.formatDecimalHours())
                                Text("@${sunEvents.sunriseAzimuth}°")
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.sunset),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(sunEvents.sunset.formatDecimalHours())
                                Text("@${sunEvents.sunsetAzimuth}°")
                            }
                        }
                    }
                    1 -> {
                        // Moon
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sunIconDrawer = rememberChartIconDrawer(Charts.Moon.Daily.Elevation)
                            Column(
                                modifier = Modifier.width(64.dp)
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
                                    text = stringResource(R.string.moon),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("${currentMoonPosition.altitude}°")
                                Text("@${currentMoonPosition.azimuth}°")
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.moonrise),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(moonEvents.moonrise.formatDecimalHours())
                                Text("@${moonEvents.moonriseAzimuth}°")
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.moonset),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(moonEvents.moonset.formatDecimalHours())
                                Text("@${moonEvents.moonsetAzimuth}°")
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
