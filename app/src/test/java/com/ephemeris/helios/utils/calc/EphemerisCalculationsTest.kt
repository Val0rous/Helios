package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs

class EphemerisCalculationsTest {

    private val rome = Coordinates(latitude = 41.9028, longitude = 12.4964, altitude = 0.0)
    private val svalbard = Coordinates(latitude = 78.2232, longitude = 15.6267, altitude = 0.0)
    private val northPole = Coordinates(latitude = 90.0, longitude = 0.0, altitude = 0.0)

    @Test
    fun lunarCulminationModes_maxAltitudeVsMeridianTransitAndZeroSpeedAtPeak() {
        // Choose a date when the Moon is crossing the celestial equator rapidly (~0.25°/hr Dec drift)
        val day = ZonedDateTime.of(2026, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        val maxAltEvents = LunarEphemeris.calculateDailyEvents(
            time = day,
            coordinates = rome,
            culminationMode = LunarEphemeris.CulminationMode.MAX_ALTITUDE,
        )
        val transitEvents = LunarEphemeris.calculateDailyEvents(
            time = day,
            coordinates = rome,
            culminationMode = LunarEphemeris.CulminationMode.MERIDIAN_TRANSIT,
        )

        val maxAltHour = maxAltEvents.culmination
        val transitHour = transitEvents.culmination
        assertNotNull(maxAltHour)
        assertNotNull(transitHour)

        // Meridian transit occurs at due South (180°)
        assertEquals(180.0, transitEvents.culminationAzimuth!!, 0.05)

        // Physical max altitude is strictly >= meridian transit altitude
        assertTrue(
            "Expected max altitude (${maxAltEvents.culminationAltitude}) >= transit altitude (${transitEvents.culminationAltitude})",
            maxAltEvents.culminationAltitude!! >= transitEvents.culminationAltitude!! - 1e-6,
        )

        // At the physical culmination instant, topocentric vertical velocity must be ~0°/min
        val posAtPeak = LunarEphemeris.getPositionAtHour(day.toLocalDate(), maxAltHour!!, rome, 0.0)
        assertEquals(0.0, posAtPeak.lunarSpeedDegPerMin, 0.0005)
    }

    @Test
    fun lunarSkippedCulminationDay_returnsNullInsteadOfBogusMidnight() {
        var skippedCount = 0
        val start = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        for (offset in 0 until 32) {
            val day = start.plusDays(offset.toLong())
            val events = LunarEphemeris.calculateDailyEvents(day, rome)
            if (events.culmination == null) {
                skippedCount++
                assertTrue(events.culminationAltitude == null && events.culminationAzimuth == null)
            } else {
                // Any reported culmination must be an authentic local maximum, not a stuck midnight endpoint
                val h = events.culmination
                val altPeak = LunarEphemeris.getPositionAtHour(day.toLocalDate(), h, rome, 0.0).altitude
                val altBefore = LunarEphemeris.getPositionAtHour(day.toLocalDate(), h - 0.05, rome, 0.0).altitude
                val altAfter = LunarEphemeris.getPositionAtHour(day.toLocalDate(), h + 0.05, rome, 0.0).altitude
                assertTrue(
                    "Culmination on ${day.toLocalDate()} at $h h was not a local peak ($altBefore, $altPeak, $altAfter)",
                    altPeak >= altBefore - 1e-4 && altPeak >= altAfter - 1e-4,
                )
            }
        }
        assertTrue("Expected at least 1 skipped lunar culmination in 32 days, found $skippedCount", skippedCount >= 1)
    }

    @Test
    fun lunarPolarGrazing_neverMissesHorizonCrossingsAtSvalbard() {
        val start = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        for (offset in 0 until 30) {
            val day = start.plusDays(offset.toLong())
            val events = LunarEphemeris.calculateDailyEvents(day, svalbard)
            if (events.isMoonDownAllDay && events.culminationAltitude != null) {
                assertTrue(
                    "Moon peaked above horizon (${events.culminationAltitude}) on ${day.toLocalDate()} yet marked down all day",
                    events.culminationAltitude < svalbard.moonApparentHorizonAlt,
                )
            }
        }
    }

    @Test
    fun solarPolarGrazing_noNaNsAndExactPoleSafeAcrossFullYear() {
        val start = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        for (dayOffset in 0 until 365) {
            val day = start.plusDays(dayOffset.toLong())
            val svalbardEvents = SolarEphemeris.calculateDailyEvents(day, svalbard)
            svalbardEvents.sunrise?.let { assertFalse("Sunrise NaN on $day", it.isNaN()) }
            svalbardEvents.sunset?.let { assertFalse("Sunset NaN on $day", it.isNaN()) }
            assertFalse("DayLength NaN on $day", svalbardEvents.dayLength.isNaN())
            assertTrue("DayLength out of [0,24] on $day: ${svalbardEvents.dayLength}", svalbardEvents.dayLength in 0.0..24.0)

            // If noon is strictly above horizon and midnight strictly below, must detect at least one crossing
            val h = svalbard.sunApparentHorizonAlt
            if (svalbardEvents.solarNoonAltitude > h + 0.05 && svalbardEvents.solarMidnightAltitude < h - 0.05) {
                assertTrue(
                    "Missed solar grazing crossing on ${day.toLocalDate()} (noon=${svalbardEvents.solarNoonAltitude}, mid=${svalbardEvents.solarMidnightAltitude})",
                    svalbardEvents.sunrise != null || svalbardEvents.sunset != null,
                )
            }
        }

        // Exact North Pole (90°N) must never throw or return NaN
        val equinox = ZonedDateTime.of(2026, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        val poleEvents = SolarEphemeris.calculateDailyEvents(equinox, northPole)
        poleEvents.sunrise?.let { assertFalse(it.isNaN()) }
        poleEvents.sunset?.let { assertFalse(it.isNaN()) }
        assertFalse(poleEvents.dayLength.isNaN())
    }

    @Test
    fun instantaneousSemidiameterOption_shiftsRiseSetAsExpected() {
        // Moon near perigee (~357,000 km -> semidiameter ~0.279° vs fixed 0.258° -> rises slightly earlier)
        val perigeeMoonDay = ZonedDateTime.of(2026, 11, 24, 12, 0, 0, 0, ZoneOffset.UTC)
        val moonFixed = LunarEphemeris.calculateDailyEvents(perigeeMoonDay, rome, useInstantaneousSemidiameter = false)
        val moonInst = LunarEphemeris.calculateDailyEvents(perigeeMoonDay, rome, useInstantaneousSemidiameter = true)
        assertNotNull(moonFixed.moonrise)
        assertNotNull(moonInst.moonrise)
        assertTrue(
            "Expected perigee moonrise earlier with instantaneous semidiameter (${moonInst.moonrise} vs ${moonFixed.moonrise})",
            moonInst.moonrise!! < moonFixed.moonrise!!,
        )
        assertTrue(
            "Expected small realistic shift (< 1 min)",
            abs(moonInst.moonrise - moonFixed.moonrise) * 60.0 < 1.0,
        )

        // Sun near perihelion (Jan 3 -> semidiameter ~0.271° vs fixed 0.266° -> rises slightly earlier)
        val perihelionSunDay = ZonedDateTime.of(2026, 1, 3, 12, 0, 0, 0, ZoneOffset.UTC)
        val sunFixed = SolarEphemeris.calculateDailyEvents(perihelionSunDay, rome, useInstantaneousSemidiameter = false)
        val sunInst = SolarEphemeris.calculateDailyEvents(perihelionSunDay, rome, useInstantaneousSemidiameter = true)
        assertNotNull(sunFixed.sunrise)
        assertNotNull(sunInst.sunrise)
        assertTrue(
            "Expected perihelion sunrise earlier with instantaneous semidiameter (${sunInst.sunrise} vs ${sunFixed.sunrise})",
            sunInst.sunrise!! < sunFixed.sunrise!!,
        )
    }

    @Test
    fun dstSpringForward_rome2026_eventsAndDurationsConsistentBeforeAndAfterTransition() {
        val romeZone = ZoneId.of("Europe/Rome")
        val beforeTransition = ZonedDateTime.of(2026, 3, 29, 0, 30, 0, 0, romeZone) // CET (+01:00)
        val afterTransition = ZonedDateTime.of(2026, 3, 29, 14, 0, 0, 0, romeZone)  // CEST (+02:00)

        val timeline = LocalDayTimeline(beforeTransition.toLocalDate(), romeZone)
        assertEquals(23.0, timeline.durationHours, 1e-6)
        assertTrue(timeline.hasDstTransition)

        val eventsBefore = SolarEphemeris.calculateDailyEvents(beforeTransition, rome)
        val eventsAfter = SolarEphemeris.calculateDailyEvents(afterTransition, rome)

        // Identical events regardless of whether queried before or after 02:00 -> 03:00 jump
        assertEquals(eventsBefore.sunrise!!, eventsAfter.sunrise!!, 1e-6)
        assertEquals(eventsBefore.solarNoon, eventsAfter.solarNoon, 1e-6)
        assertEquals(eventsBefore.sunset!!, eventsAfter.sunset!!, 1e-6)
        assertEquals(eventsBefore.dayLength, eventsAfter.dayLength, 1e-6)

        // In CEST (+2), solar noon in Rome (12.5°E) is ~13:15 local wall-clock time (not 12:15 CET)
        assertTrue("Expected CEST solar noon around 13:15, got ${eventsAfter.solarNoon}", eventsAfter.solarNoon in 13.0..13.5)

        // Jumping via solarNoonElapsedHours lands on the exact wall-clock noon in CEST (+02:00)
        val noonZdt = timeline.elapsedHourToZonedDateTime(eventsAfter.solarNoonElapsedHours)
        assertEquals(7200, noonZdt.offset.totalSeconds)
        val noonWallFromZdt = noonZdt.hour + noonZdt.minute / 60.0 + noonZdt.second / 3600.0
        assertEquals(eventsAfter.solarNoon, noonWallFromZdt, 1.0 / 3600.0)

        // Durations on a 23h spring-forward day must sum to 23.0h
        val durations = SolarEphemeris.calculateDailyDurations(eventsAfter, rome, time = afterTransition)
        val totalHours = durations.night.total + durations.astronomicalTwilight.total +
            durations.nauticalTwilight.total + durations.civilTwilight.total + eventsAfter.dayLength
        assertEquals(23.0, totalHours, 1e-3)

        // Lunar events before & after transition are identical and daily curve bridges gap cleanly
        val lunarBefore = LunarEphemeris.calculateDailyEvents(beforeTransition, rome)
        val lunarAfter = LunarEphemeris.calculateDailyEvents(afterTransition, rome)
        assertEquals(lunarBefore.moonDuration, lunarAfter.moonDuration, 1e-6)
        assertEquals(lunarBefore.culmination, lunarAfter.culmination)

        val lunarModel = LunarEphemeris.createDailyModel(beforeTransition.toLocalDate(), rome, romeZone)
        val hours = DoubleArray(1441) { it / 60.0 }
        val elev = FloatArray(1441)
        val az = FloatArray(1441)
        val dist = DoubleArray(1441)
        lunarModel.fillDailyCurve(hours, elev, az, dist)
        assertFalse(elev.any { it.isNaN() })
        // Across the skipped 02:00..03:00 hour (indices 120..180), elevation is pinned flat to transition instant
        assertEquals(elev[120], elev[150], 1e-3f)
    }

    @Test
    fun dstFallBack_rome2026_eventsAndDurationsSumTo25Hours() {
        val romeZone = ZoneId.of("Europe/Rome")
        val beforeTransition = ZonedDateTime.of(2026, 10, 25, 0, 30, 0, 0, romeZone) // CEST (+02:00)
        val afterTransition = ZonedDateTime.of(2026, 10, 25, 14, 0, 0, 0, romeZone)  // CET (+01:00)

        val timeline = LocalDayTimeline(beforeTransition.toLocalDate(), romeZone)
        assertEquals(25.0, timeline.durationHours, 1e-6)

        val eventsBefore = SolarEphemeris.calculateDailyEvents(beforeTransition, rome)
        val eventsAfter = SolarEphemeris.calculateDailyEvents(afterTransition, rome)

        assertEquals(eventsBefore.sunrise!!, eventsAfter.sunrise!!, 1e-6)
        assertEquals(eventsBefore.solarNoon, eventsAfter.solarNoon, 1e-6)
        assertEquals(eventsBefore.sunset!!, eventsAfter.sunset!!, 1e-6)
        assertEquals(eventsBefore.dayLength, eventsAfter.dayLength, 1e-6)

        // In CET (+1) on Oct 25, Rome solar noon is ~11:54 local wall-clock time (not 12:54 CEST)
        assertTrue("Expected CET solar noon around 11:54, got ${eventsAfter.solarNoon}", eventsAfter.solarNoon in 11.7..12.2)

        val durations = SolarEphemeris.calculateDailyDurations(eventsAfter, rome, time = afterTransition)
        val totalHours = durations.night.total + durations.astronomicalTwilight.total +
            durations.nauticalTwilight.total + durations.civilTwilight.total + eventsAfter.dayLength
        assertEquals(25.0, totalHours, 1e-3)
    }

    @Test
    fun dstMidnightTransition_havana2026_anchorOffsetExact() {
        val havanaZone = ZoneId.of("America/Havana")
        val havana = Coordinates(latitude = 23.1136, longitude = -82.3666, altitude = 0.0)
        val dstDay = ZonedDateTime.of(2026, 3, 8, 12, 0, 0, 0, havanaZone)

        val timeline = LocalDayTimeline(dstDay.toLocalDate(), havanaZone)
        assertEquals(23.0, timeline.durationHours, 1e-6)

        val events = SolarEphemeris.calculateDailyEvents(dstDay, havana)
        // Havana (82.37°W) in CDT (-04:00) has solar noon around 13:40 local time
        assertTrue("Expected CDT solar noon around 13:40 in Havana, got ${events.solarNoon}", events.solarNoon in 13.4..13.9)
        val noonZdt = timeline.elapsedHourToZonedDateTime(events.solarNoonElapsedHours)
        val posAtNoon = SolarEphemeris.calculatePosition(noonZdt, havana)
        assertEquals(events.solarNoonAltitude, posAtNoon.altitude, 0.02)
    }

    @Test
    fun lunarTopocentricDeclination_accountsForDiurnalParallax() {
        // Moon near culmination south of zenith in Rome (41.9°N) -> parallax pulls apparent Moon southward
        val day = ZonedDateTime.of(2026, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        val events = LunarEphemeris.calculateDailyEvents(day, rome)
        val peakHour = events.culmination!!
        val pos = LunarEphemeris.getPositionAtHour(day.toLocalDate(), peakHour, rome, 0.0)

        val parallaxDelta = pos.declination - pos.topocentricDeclination
        assertTrue(
            "Expected southward diurnal parallax in Northern Hemisphere (geo=${pos.declination}, topo=${pos.topocentricDeclination})",
            parallaxDelta in 0.15..1.1,
        )
    }

    @Test
    fun subtropicalLunarTransitAzimuth_flipsNorthWhenDeclinationExceedsLatitude() {
        // Observer at 25.0°N during Major Lunar Standstill (2026): Moon swings between ~-28° and ~+28°
        val subtropics = Coordinates(latitude = 25.0, longitude = 0.0, altitude = 0.0)
        val start = ZonedDateTime.of(2026, 11, 1, 12, 0, 0, 0, ZoneOffset.UTC)
        var sawNorthTransit = false
        var sawSouthTransit = false

        for (offset in 0 until 29) {
            val day = start.plusDays(offset.toLong())
            val events = LunarEphemeris.calculateDailyEvents(day, subtropics)
            val transitAz = events.transitAzimuth ?: continue
            val normalizedAz = ((transitAz % 360.0) + 360.0) % 360.0
            val isNorth = normalizedAz < 1.0 || normalizedAz > 359.0
            val isSouth = abs(normalizedAz - 180.0) < 1.0
            assertTrue("Upper meridian transit must be strictly North or South, got $transitAz", isNorth || isSouth)
            if (isNorth) sawNorthTransit = true
            if (isSouth) sawSouthTransit = true
        }

        assertTrue("Expected Moon to transit North of zenith at 25°N during Major Standstill", sawNorthTransit)
        assertTrue("Expected Moon to transit South of zenith at 25°N during month", sawSouthTransit)
    }
}
