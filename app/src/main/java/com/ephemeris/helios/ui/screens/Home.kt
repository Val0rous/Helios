package com.ephemeris.helios.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.composables.cards.SmallCardRow
import com.ephemeris.helios.ui.composables.entries.SeasonalEntry
import com.ephemeris.helios.ui.theme.LocalCustomColors
import com.ephemeris.helios.utils.calc.SeasonalEphemeris

@Composable
fun Home(
    seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SmallCardRow(
                leftCard = {
                    SeasonalEntry(
                        title = R.string.equinoxes,
                        color = MaterialTheme.colorScheme.primary,
                        dateTimeTop = seasonalEvents.marchEquinox,
                        daylightTop = seasonalDailyEvents.marchEquinoxDaylight,
                        sunAngleTop = seasonalDailyEvents.marchEquinoxSunAngle,
                        dateTimeBottom = seasonalEvents.septemberEquinox,
                        daylightBottom = seasonalDailyEvents.septemberEquinoxDaylight,
                        sunAngleBottom = seasonalDailyEvents.septemberEquinoxSunAngle
                    )
                },
                rightCard = {
                    SeasonalEntry(
                        title = R.string.solstices,
                        color = LocalCustomColors.current.nightPrimary,
                        dateTimeTop = seasonalEvents.juneSolstice,
                        daylightTop = seasonalDailyEvents.juneSolsticeDaylight,
                        sunAngleTop = seasonalDailyEvents.juneSolsticeSunAngle,
                        dateTimeBottom = seasonalEvents.decemberSolstice,
                        daylightBottom = seasonalDailyEvents.decemberSolsticeDaylight,
                        sunAngleBottom = seasonalDailyEvents.decemberSolsticeSunAngle
                    )
                }
            )
        }
    }
}