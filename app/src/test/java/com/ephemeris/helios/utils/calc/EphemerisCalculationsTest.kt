package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import com.ephemeris.helios.utils.location.estimateHistoricalOzone
import com.ephemeris.helios.utils.location.reverseEngineerOzone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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

        // Default Coordinates keeps fixed mean semidiameter; Coordinates.useInstantaneousSemidiameter = true propagates automatically
        assertFalse(rome.useInstantaneousSemidiameter)
        val romeInstant = rome.copy(useInstantaneousSemidiameter = true)
        assertEquals(moonFixed.moonrise, LunarEphemeris.calculateDailyEvents(perigeeMoonDay, rome).moonrise!!, 1e-9)
        assertEquals(moonInst.moonrise, LunarEphemeris.calculateDailyEvents(perigeeMoonDay, romeInstant).moonrise!!, 1e-9)
        assertEquals(sunFixed.sunrise, SolarEphemeris.calculateDailyEvents(perihelionSunDay, rome).sunrise!!, 1e-9)
        assertEquals(sunInst.sunrise, SolarEphemeris.calculateDailyEvents(perihelionSunDay, romeInstant).sunrise!!, 1e-9)
        assertTrue(romeInstant.moonApparentHorizonAlt(357_000.0) < rome.moonApparentHorizonAlt)
        assertTrue(romeInstant.sunApparentHorizonAlt(0.9833) < rome.sunApparentHorizonAlt)
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

    @Test
    fun temisUvModel_matchesPublishedBenchmarksAndSmoothAtHorizon() {
        // KNMI TEMIS v2.x clear-sky sea-level benchmarks at 300 DU, 1.0 AU
        val uvi90 = SunMetrics.calculateClearSkyUvi(90.0, 0.0, 1.0, 300.0)
        val uvi60 = SunMetrics.calculateClearSkyUvi(60.0, 0.0, 1.0, 300.0)
        val uvi30 = SunMetrics.calculateClearSkyUvi(30.0, 0.0, 1.0, 300.0)
        val uvi15 = SunMetrics.calculateClearSkyUvi(15.0, 0.0, 1.0, 300.0)
        val uvi0 = SunMetrics.calculateClearSkyUvi(0.0, 0.0, 1.0, 300.0)
        val uviCivil = SunMetrics.calculateClearSkyUvi(-3.0, 0.0, 1.0, 300.0)
        val uviNight = SunMetrics.calculateClearSkyUvi(-12.5, 0.0, 1.0, 300.0)

        assertEquals(11.80, uvi90, 0.05)
        assertTrue("Expected 60° UVI in 7.6..8.1, got $uvi60", uvi60 in 7.6..8.1)
        assertTrue("Expected 30° UVI in 1.65..1.95, got $uvi30", uvi30 in 1.65..1.95)
        assertTrue("Expected 15° UVI in 0.35..0.50, got $uvi15", uvi15 in 0.35..0.50)
        assertTrue("Expected smooth civil-twilight diffuse UV decay, got 0°=$uvi0, -3°=$uviCivil", uviCivil > 0.0 && uviCivil < uvi0 && uvi0 < 0.06)
        assertEquals(0.0, uviNight, 1e-9)

        // C1 slope continuity across 0° (left & right derivatives match within 2%)
        val eps = 0.01
        val slopeLeft = (uvi0 - SunMetrics.calculateClearSkyUvi(-eps, 0.0, 1.0, 300.0)) / eps
        val slopeRight = (SunMetrics.calculateClearSkyUvi(eps, 0.0, 1.0, 300.0) - uvi0) / eps
        assertEquals("UV slope kink at 0° (left=$slopeLeft, right=$slopeRight)", slopeRight, slopeLeft, slopeRight * 0.02)

        // Nonlinear ozone amplification (erythemal RAF ~1.2-1.3 -> halving ozone more than doubles UVI)
        val uviOzoneHole = SunMetrics.calculateClearSkyUvi(30.0, 0.0, 1.0, 150.0)
        val rafRatio = uviOzoneHole / uvi30
        assertTrue("Expected nonlinear ozone amplification > 2.2x, got $rafRatio", rafRatio > 2.2)

        // Perihelion vs aphelion (~7% difference)
        val uviPerihelion = SunMetrics.calculateClearSkyUvi(60.0, 0.0, 0.9833, 300.0)
        val uviAphelion = SunMetrics.calculateClearSkyUvi(60.0, 0.0, 1.0167, 300.0)
        assertEquals((1.0167 / 0.9833) * (1.0167 / 0.9833), uviPerihelion / uviAphelion, 1e-4)

        // Single-point helper matches batch calculateMetrics exactly
        val batchResult = SunMetrics.calculateMetrics(
            sunElevationDeg = 60.0,
            observerAltitudeMeters = 1200.0,
            distanceAu = 0.99,
            ozoneDU = 285.0,
        )
        val directResult = SunMetrics.calculateClearSkyUvi(60.0, 1200.0, 0.99, 285.0)
        assertEquals(directResult, batchResult.uvIntensity, 1e-4)
    }

    @Test
    fun altitudeUvAndTroposphericOzone_physicalScalingWithoutDoubleCounting() {
        val date = LocalDate.of(2026, 6, 21)

        // 1. Sea level vs 1,000 m in Rome: realistic ~6.5–7.5%/km total clear-sky UV gain
        val o3Sea = estimateHistoricalOzone(41.9, date, longitude = 12.5, altitudeMeters = 0.0)
        val o3OneKm = estimateHistoricalOzone(41.9, date, longitude = 12.5, altitudeMeters = 1000.0)
        val uviSea = SunMetrics.calculateClearSkyUvi(70.0, 0.0, 1.0, o3Sea)
        val uviOneKm = SunMetrics.calculateClearSkyUvi(70.0, 1000.0, 1.0, o3OneKm)
        val gain1km = (uviOneKm / uviSea) - 1.0
        assertTrue("Expected 1 km UV gain in 5.5%..8.0%, got ${gain1km * 100}%", gain1km in 0.055..0.080)

        // 2. La Paz (3,650 m): realistic Altiplano tropospheric drop + ~20–28% total UV gain
        val janDate = LocalDate.of(2026, 1, 15)
        val o3LaPazSea = estimateHistoricalOzone(-16.5, janDate, longitude = -68.15, altitudeMeters = 0.0)
        val o3LaPazHigh = estimateHistoricalOzone(-16.5, janDate, longitude = -68.15, altitudeMeters = 3650.0)
        val tropDrop = o3LaPazSea - o3LaPazHigh
        assertTrue("Expected tropospheric O3 drop at 3650m in 6..15 DU, got $tropDrop", tropDrop in 6.0..15.0)
        val uviLaPazSea = SunMetrics.calculateClearSkyUvi(85.0, 0.0, 0.984, o3LaPazSea)
        val uviLaPazHigh = SunMetrics.calculateClearSkyUvi(85.0, 3650.0, 0.984, o3LaPazHigh)
        val gainLaPaz = (uviLaPazHigh / uviLaPazSea) - 1.0
        assertTrue("Expected La Paz UV gain in 19%..29%, got ${gainLaPaz * 100}%", gainLaPaz in 0.19..0.29)

        // 3. Mount Everest (8,849 m): stratosphere remains intact (> 220 DU) and UV gain stays physically bounded
        val o3EverestSea = estimateHistoricalOzone(27.99, date, longitude = 86.93, altitudeMeters = 0.0)
        val o3EverestTop = estimateHistoricalOzone(27.99, date, longitude = 86.93, altitudeMeters = 8849.0)
        assertTrue("Stratospheric ozone must stay intact on Everest, got $o3EverestTop DU", o3EverestTop > 220.0)
        val uviEverestSea = SunMetrics.calculateClearSkyUvi(80.0, 0.0, 1.016, o3EverestSea)
        val uviEverestTop = SunMetrics.calculateClearSkyUvi(80.0, 8849.0, 1.016, o3EverestTop)
        val gainEverest = (uviEverestTop / uviEverestSea) - 1.0
        assertTrue("Expected Everest UV gain bounded in 35%..52%, got ${gainEverest * 100}%", gainEverest in 0.35..0.52)

        // 4. Dead Sea (-430 m): slightly thicker tropospheric ozone and strictly lower UV than sea level
        val o3DeadSeaZero = estimateHistoricalOzone(31.5, date, longitude = 35.5, altitudeMeters = 0.0)
        val o3DeadSeaLow = estimateHistoricalOzone(31.5, date, longitude = 35.5, altitudeMeters = -430.0)
        assertTrue("Dead Sea should have slightly more tropospheric ozone", o3DeadSeaLow > o3DeadSeaZero)
        val uviDeadSeaZero = SunMetrics.calculateClearSkyUvi(75.0, 0.0, 1.016, o3DeadSeaZero)
        val uviDeadSeaLow = SunMetrics.calculateClearSkyUvi(75.0, -430.0, 1.016, o3DeadSeaLow)
        assertTrue("Dead Sea UV should be lower than sea level", uviDeadSeaLow < uviDeadSeaZero)
    }

    @Test
    fun ozoneClimatology_globalPatternsContinuityAndNoPolarCliffs() {
        // 1. Tropics: ~245–275 DU year-round with modest seasonal swing
        val eqValues = (1..12).map { m -> estimateHistoricalOzone(0.0, LocalDate.of(2026, m, 15)) }
        assertTrue("Equatorial ozone out of 240..275 DU: $eqValues", eqValues.all { it in 240.0..275.0 })
        assertTrue("Equatorial seasonal swing too large: ${eqValues.max() - eqValues.min()}", (eqValues.max() - eqValues.min()) < 25.0)

        // 2. Northern Hemisphere mid-latitudes (Rome): thick spring peak, thin autumn trough
        val romeSpring = estimateHistoricalOzone(41.9, LocalDate.of(2026, 3, 25), longitude = 12.5)
        val romeAutumn = estimateHistoricalOzone(41.9, LocalDate.of(2026, 10, 15), longitude = 12.5)
        assertTrue("Expected Rome spring ($romeSpring) >> autumn ($romeAutumn)", romeSpring - romeAutumn > 45.0)

        // 3. Arctic spring (Svalbard in April): naturally thick column (> 360 DU), no bogus dent
        val svalbardApril = estimateHistoricalOzone(78.2, LocalDate.of(2026, 4, 15), longitude = 15.6)
        assertTrue("Expected thick Arctic spring ozone > 360 DU, got $svalbardApril", svalbardApril > 360.0)

        // 4. Longitudinal planetary wave: East Asia ridge > Europe trough in NH spring
        val eastAsia = estimateHistoricalOzone(45.0, LocalDate.of(2026, 3, 15), longitude = 125.0)
        val europe = estimateHistoricalOzone(45.0, LocalDate.of(2026, 3, 15), longitude = 12.0)
        assertTrue("Expected East Asia ridge ($eastAsia) > Europe trough ($europe) by > 20 DU", eastAsia - europe > 20.0)

        // 5. Smooth continuity across ±60° latitude, Prime Meridian, Date Line, and New Year's Eve
        val oct15 = LocalDate.of(2026, 10, 15)
        val apr15 = LocalDate.of(2026, 4, 15)
        assertTrue(abs(estimateHistoricalOzone(-59.9, oct15) - estimateHistoricalOzone(-60.1, oct15)) < 3.0)
        assertTrue(abs(estimateHistoricalOzone(59.9, apr15) - estimateHistoricalOzone(60.1, apr15)) < 3.0)
        assertTrue(abs(estimateHistoricalOzone(45.0, apr15, longitude = -0.01) - estimateHistoricalOzone(45.0, apr15, longitude = 0.01)) < 0.5)
        assertTrue(abs(estimateHistoricalOzone(45.0, apr15, longitude = -179.99) - estimateHistoricalOzone(45.0, apr15, longitude = 179.99)) < 0.5)
        assertTrue(abs(estimateHistoricalOzone(45.0, LocalDate.of(2026, 12, 31)) - estimateHistoricalOzone(45.0, LocalDate.of(2027, 1, 1))) < 2.0)

        // 6. Exact poles must be strictly independent of longitude
        assertEquals(
            estimateHistoricalOzone(90.0, apr15, longitude = 0.0),
            estimateHistoricalOzone(90.0, apr15, longitude = 135.0),
            1e-6,
        )
        assertEquals(
            estimateHistoricalOzone(-90.0, oct15, longitude = -75.0),
            estimateHistoricalOzone(-90.0, oct15, longitude = 135.0),
            1e-6,
        )
    }

    @Test
    fun antarcticOzoneHole_historicalDepletionAndFutureRecoveryTimeline() {
        val southPoleAlt = 2835.0
        val o3_1970 = estimateHistoricalOzone(-90.0, LocalDate.of(1970, 10, 15), altitudeMeters = southPoleAlt)
        val o3_1985 = estimateHistoricalOzone(-90.0, LocalDate.of(1985, 10, 15), altitudeMeters = southPoleAlt)
        val o3_2000 = estimateHistoricalOzone(-90.0, LocalDate.of(2000, 10, 15), altitudeMeters = southPoleAlt)
        val o3_2007 = estimateHistoricalOzone(-90.0, LocalDate.of(2007, 10, 15), altitudeMeters = southPoleAlt)
        val o3_2026 = estimateHistoricalOzone(-90.0, LocalDate.of(2026, 10, 15), altitudeMeters = southPoleAlt)
        val o3_2070 = estimateHistoricalOzone(-90.0, LocalDate.of(2070, 10, 15), altitudeMeters = southPoleAlt)

        // Pre-1979 baseline: no ozone hole (~270–295 DU at South Pole)
        assertTrue("Expected 1970 South Pole Oct ozone in 265..295 DU, got $o3_1970", o3_1970 in 265.0..295.0)
        // 1985 discovery era: clearly depleted vs 1970, not yet at 2000 minimum
        assertTrue("Expected 1985 ($o3_1985) between 2000 ($o3_2000) and 1970 ($o3_1970)", o3_1985 in (o3_2000 + 40.0)..(o3_1970 - 30.0))
        // ~2000 peak depletion: severe Antarctic hole (~135–165 DU)
        assertTrue("Expected 2000 peak ozone hole in 135..165 DU, got $o3_2000", o3_2000 in 135.0..165.0)
        // 2007 Ziemke reference epoch close to 2000 peak
        assertTrue("Expected 2007 ($o3_2007) close to 2000 ($o3_2000)", abs(o3_2007 - o3_2000) < 10.0)
        // 2026 WMO partial recovery: noticeably above 2000 peak, still well below 1970 baseline
        assertTrue("Expected 2026 ($o3_2026) partially recovered between 2000 ($o3_2000) and 1970 ($o3_1970)", o3_2026 > o3_2000 + 25.0 && o3_2026 < o3_1970 - 60.0)
        // 2070 post-recovery: returns to pre-1979 baseline
        assertEquals(o3_1970, o3_2070, 2.0)

        // Outside austral spring (Feb 15): no ozone hole in any year
        val feb1970 = estimateHistoricalOzone(-90.0, LocalDate.of(1970, 2, 15), altitudeMeters = southPoleAlt)
        val feb2000 = estimateHistoricalOzone(-90.0, LocalDate.of(2000, 2, 15), altitudeMeters = southPoleAlt)
        assertTrue("Expected no Antarctic ozone hole in February (1970=$feb1970, 2000=$feb2000)", abs(feb1970 - feb2000) < 15.0)
    }

    @Test
    fun reverseEngineerOzone_invertsTemisUvModelAccurately() {
        val cases = listOf(
            Triple(65.0, 0.0, 300.0),
            Triple(35.0, 0.0, 160.0),
            Triple(80.0, 3650.0, 245.0),
            Triple(50.0, 1500.0, 420.0),
        )
        for ((elevDeg, altM, trueOzone) in cases) {
            val distanceAu = 0.992
            val uvi = SunMetrics.calculateClearSkyUvi(
                sunElevationDeg = elevDeg,
                observerAltitudeMeters = altM,
                distanceAu = distanceAu,
                ozoneDU = trueOzone,
            )
            val recovered = reverseEngineerOzone(
                hourlyClearSkyUv = listOf(0.0, uvi * 0.6, uvi, uvi * 0.4),
                solarNoonAltitudeDegrees = elevDeg,
                elevationMeters = altM,
                distanceAu = distanceAu,
            )
            assertEquals("Failed to invert ozone for ($elevDeg°, ${altM}m, $trueOzone DU)", trueOzone, recovered, 0.05)
        }
    }

    @Test
    fun sunAndMoonMetrics_c1SmoothAcrossHorizonAndZeroElevation() {
        val eps = 0.02

        // 1. Solar Illuminance (Global & Diffuse) C1 smooth across 0° at sea level AND 3650 m
        for (altMeters in listOf(0.0, 3650.0)) {
            val mMinus = SunMetrics.calculateMetrics(-eps, altMeters)
            val mZero = SunMetrics.calculateMetrics(0.0, altMeters)
            val mPlus = SunMetrics.calculateMetrics(eps, altMeters)

            val luxLeft = (mZero.globalIlluminance - mMinus.globalIlluminance) / eps
            val luxRight = (mPlus.globalIlluminance - mZero.globalIlluminance) / eps
            assertEquals(
                "Solar illuminance slope kink at 0° (${altMeters}m): left=$luxLeft, right=$luxRight",
                luxRight,
                luxLeft,
                luxRight * 0.03,
            )

            // Also smooth across apparent sunrise (-0.833°)
            val h = -0.833
            val mBelow = SunMetrics.calculateMetrics(h - eps, altMeters, horizonAltDeg = h)
            val mAt = SunMetrics.calculateMetrics(h, altMeters, horizonAltDeg = h)
            val mAbove = SunMetrics.calculateMetrics(h + eps, altMeters, horizonAltDeg = h)
            val sLeft = (mAt.globalIlluminance - mBelow.globalIlluminance) / eps
            val sRight = (mAbove.globalIlluminance - mAt.globalIlluminance) / eps
            assertEquals(
                "Solar illuminance kink at sunrise (${altMeters}m): left=$sLeft, right=$sRight",
                sRight,
                sLeft,
                sRight * 0.03,
            )

            // Air Mass C1 smooth across 0°
            val amLeft = (mZero.airMass - mMinus.airMass) / eps
            val amRight = (mPlus.airMass - mZero.airMass) / eps
            assertEquals("Solar Air Mass kink at 0°", amRight, amLeft, abs(amRight) * 0.03)
        }

        // 2. Lunar Illuminance C1 smooth across 0° and apparent moonrise
        val testDate = LocalDate.of(2026, 11, 24)
        val elevs = floatArrayOf(-0.853f, -0.833f, -0.813f, -0.02f, 0.0f, 0.02f)
        val hours = DoubleArray(elevs.size) { 12.0 }
        val dists = DoubleArray(elevs.size) { 384400.0 }
        val outLux = FloatArray(elevs.size)
        val outShadow = FloatArray(elevs.size)
        val outCt = FloatArray(elevs.size)
        val outAm = FloatArray(elevs.size)

        MoonMetrics.calculateMetrics(
            date = testDate,
            hoursCalc = hours,
            zoneId = ZoneOffset.UTC,
            moonElevationsDeg = elevs,
            distancesKm = dists,
            observerAltitudeMeters = 0.0,
            horizonAltDeg = -0.833,
            outIlluminance = outLux,
            outShadowRatio = outShadow,
            outColorTemp = outCt,
            outAirMass = outAm,
        )

        val moonZeroLeft = (outLux[4] - outLux[3]) / eps
        val moonZeroRight = (outLux[5] - outLux[4]) / eps
        assertEquals(
            "Lunar illuminance slope kink at 0°: left=$moonZeroLeft, right=$moonZeroRight",
            moonZeroRight,
            moonZeroLeft,
            moonZeroRight * 0.03,
        )
        val moonHorizLeft = (outLux[1] - outLux[0]) / eps
        val moonHorizRight = (outLux[2] - outLux[1]) / eps
        assertEquals(
            "Lunar illuminance kink at moonrise: left=$moonHorizLeft, right=$moonHorizRight",
            moonHorizRight,
            moonHorizLeft,
            moonHorizRight * 0.03,
        )
    }
}
