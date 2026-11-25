package com.ephemeris.helios.ui.composables.cards

import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NightAndSolarMidnightTest {

    @Test
    fun eotDriftSkipsSolarMidnightOnDecemberPrimeMeridian() {
        // Near Greenwich (lon = 0°, UTC), Equation of Time crosses zero around Dec 24–25
        // while decreasing ~30s/day, pushing one calendar day between 23:59:xx (prev day)
        // and 00:00:xx (next day) so it contains zero solar midnights.
        val greenwich = Coordinates(latitude = 51.4769, longitude = 0.0)
        val skippedDays = (20..28).mapNotNull { dayOfMonth ->
            val time = ZonedDateTime.of(2026, 12, dayOfMonth, 12, 0, 0, 0, ZoneOffset.UTC)
            val events = SolarEphemeris.calculateDailyEvents(time, greenwich)
            if (events.solarMidnight == null) {
                assertNull(events.solarMidnightElapsedHours)
                val durations = SolarEphemeris.calculateDailyDurations(events, greenwich, time = time)
                assertTrue(
                    "Night duration should remain positive on skipped-midnight winter day",
                    durations.night.total > 10.0,
                )
                dayOfMonth
            } else {
                assertNotNull(events.solarMidnightElapsedHours)
                assertTrue(events.solarMidnightElapsedHours!! in 0.0..<24.0)
                null
            }
        }

        assertTrue(
            "Expected at least one December day at lon=0° UTC with no solar midnight due to EoT drift",
            skippedDays.isNotEmpty(),
        )
    }

    @Test
    fun standardDaySolarMidnightMatchesLowerTransitPosition() {
        val rome = Coordinates(latitude = 41.9028, longitude = 12.4964)
        val time = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, ZoneOffset.ofHours(2))
        val events = SolarEphemeris.calculateDailyEvents(time, rome)

        assertNotNull(events.solarMidnight)
        assertNotNull(events.solarMidnightElapsedHours)
        assertTrue(events.solarMidnightElapsedHours!! in 0.0..<24.0)
        assertTrue("Rome summer solar midnight altitude should be below horizon", events.solarMidnightAltitude < -18.0)
    }

    @Test
    fun fastEotInSeptemberProducesTwoSolarMidnightsInOneDay() {
        // Around mid-September, EoT increases by ~+21s/day (true solar day ~23h 59m 39s).
        // Choosing a longitude where solar midnight crosses 00:00 produces two midnights
        // on the same calendar day (just after 00:00 and just before 24:00).
        val shifted = Coordinates(latitude = 45.0, longitude = -1.35)
        val doubleDays = (14..22).mapNotNull { dayOfMonth ->
            val time = ZonedDateTime.of(2026, 9, dayOfMonth, 12, 0, 0, 0, ZoneOffset.UTC)
            val events = SolarEphemeris.calculateDailyEvents(time, shifted)
            if (events.solarMidnights.size == 2) {
                val first = events.solarMidnights[0]
                val second = events.solarMidnights[1]
                assertTrue("First midnight should be right after 00:00", first.elapsedHours < 0.05)
                assertTrue("Second midnight should be right before 24:00", second.elapsedHours > 23.95)
                dayOfMonth
            } else {
                null
            }
        }

        assertTrue(
            "Expected a mid-September day with 2 solar midnights due to fast EoT",
            doubleDays.isNotEmpty(),
        )
    }

    @Test
    fun dstFallBack25HourDayCanContainTwoSolarMidnights() {
        // On a 25-hour DST fall-back Sunday (Oct 25, 2026 in Europe/Warsaw, CEST -> CET),
        // solar midnight occurs ~00:20 CEST (elapsed ~0.33h) and again 24h later at ~23:20 CET
        // (elapsed ~24.33h < 25.0h) on the same calendar day.
        val warsaw = Coordinates(latitude = 52.2297, longitude = 21.0122)
        val fallBack = ZonedDateTime.of(2026, 10, 25, 12, 0, 0, 0, java.time.ZoneId.of("Europe/Warsaw"))
        val events = SolarEphemeris.calculateDailyEvents(fallBack, warsaw)

        assertTrue(
            "Expected 2 solar midnights on Warsaw's 25-hour fall-back day, got ${events.solarMidnights.size}",
            events.solarMidnights.size == 2,
        )
        assertTrue(events.solarMidnights[0].elapsedHours < 1.0)
        assertTrue(events.solarMidnights[1].elapsedHours > 24.0)
    }

    @Test
    fun aboveHorizonThresholdMatchesReversePolarNightRule() {
        val seaLevel = Coordinates(latitude = 78.2232, longitude = 15.6267, altitude = 0.0, useHorizonDip = false)
        val threshold = resolveCulminationHorizonThreshold(EphemerisType.SUN, seaLevel)

        // Above sunrise threshold (-0.833°) -> Midnight Sun (red)
        assertTrue(isSolarMidnightAboveHorizon(4.8, threshold))
        assertTrue(isSolarMidnightAboveHorizon(-0.5, threshold))
        // Below sunrise threshold -> Sun sets normally (not red)
        assertFalse(isSolarMidnightAboveHorizon(-1.2, threshold))
        assertFalse(isSolarMidnightAboveHorizon(-32.4, threshold))
        assertFalse(isSolarMidnightAboveHorizon(null, threshold))

        // Mountain peak with horizon dip lowers the sunrise threshold
        val mountain = Coordinates(latitude = 78.2232, longitude = 15.6267, altitude = 3000.0, useHorizonDip = true)
        val dippedThreshold = resolveCulminationHorizonThreshold(EphemerisType.SUN, mountain)
        assertTrue(dippedThreshold < threshold - 1.0)
        assertTrue(isSolarMidnightAboveHorizon(-1.5, dippedThreshold))
    }
}
