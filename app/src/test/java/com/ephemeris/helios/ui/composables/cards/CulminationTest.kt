package com.ephemeris.helios.ui.composables.cards

import com.ephemeris.helios.utils.EphemerisType
import com.ephemeris.helios.utils.calc.LunarEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.formatAzimuth
import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class CulminationTest {

    @Test
    fun sunThreshold_usesStandardSunriseThresholdAtSeaLevel() {
        val seaLevel = Coordinates(41.9, 12.5, altitude = 0.0, useHorizonDip = false)
        val threshold = resolveCulminationHorizonThreshold(EphemerisType.SUN, seaLevel)
        assertEquals(SolarEphemeris.ALT_SUNRISE_SUNSET, threshold, 1e-6)
        assertTrue(isCulminationBelowHorizon(-0.84, threshold))
        assertFalse(isCulminationBelowHorizon(-0.83, threshold))
    }

    @Test
    fun sunThreshold_followsApparentHorizonOnMountainPeakWhenEnabled() {
        val mountainEnabled = Coordinates(70.0, 20.0, altitude = 4000.0, useHorizonDip = true)
        val mountainDisabled = mountainEnabled.copy(useHorizonDip = false)

        val dipThreshold = resolveCulminationHorizonThreshold(EphemerisType.SUN, mountainEnabled)
        val flatThreshold = resolveCulminationHorizonThreshold(EphemerisType.SUN, mountainDisabled)
        val forcedStandard = resolveCulminationHorizonThreshold(
            type = EphemerisType.SUN,
            coordinates = mountainEnabled,
            followApparentHorizonSetting = false,
        )

        // At 4000m, dip ≈ 0.032 * sqrt(4000) ≈ 2.024°, so apparent horizon ≈ -2.857°
        assertEquals(mountainEnabled.sunApparentHorizonAlt, dipThreshold, 1e-6)
        assertEquals(SolarEphemeris.ALT_SUNRISE_SUNSET, flatThreshold, 1e-6)
        assertEquals(SolarEphemeris.ALT_SUNRISE_SUNSET, forcedStandard, 1e-6)

        // -1.5° is visible from the 4000m peak when apparent horizon is enabled, but below standard horizon
        assertFalse(isCulminationBelowHorizon(-1.5, dipThreshold))
        assertTrue(isCulminationBelowHorizon(-1.5, flatThreshold))
        assertTrue(isCulminationBelowHorizon(-3.0, dipThreshold))
    }

    @Test
    fun moonThreshold_usesLunarHorizonAndRespectsHorizonDip() {
        val peak = Coordinates(78.22, 15.63, altitude = 2500.0, useHorizonDip = true)
        val seaLevel = peak.copy(useHorizonDip = false)

        val dipThreshold = resolveCulminationHorizonThreshold(EphemerisType.MOON, peak)
        val standardThreshold = resolveCulminationHorizonThreshold(EphemerisType.MOON, seaLevel)

        // At 2500m, dip = 0.032 * 50 = 1.6°, so lunar apparent horizon = -0.825 - 1.6 = -2.425°
        assertEquals(LunarEphemeris.ALT_MOONRISE_MOONSET, standardThreshold, 1e-6)
        assertEquals(LunarEphemeris.ALT_MOONRISE_MOONSET - 1.6, dipThreshold, 1e-6)
        assertFalse(isCulminationBelowHorizon(-2.0, dipThreshold))
        assertTrue(isCulminationBelowHorizon(-2.0, standardThreshold))
    }

    @Test
    fun missingOrNaNAltitude_isNeverMarkedBelowHorizon() {
        assertFalse(isCulminationBelowHorizon(null, SolarEphemeris.ALT_SUNRISE_SUNSET))
        assertFalse(isCulminationBelowHorizon(Double.NaN, SolarEphemeris.ALT_SUNRISE_SUNSET))
    }

    @Test
    fun formatAzimuth_alwaysFormatsOneDecimalAndAllSixteenDirections() {
        val expected = listOf(
            0.0 to "0.0° N",
            22.5 to "22.5° NNE",
            45.0 to "45.0° NE",
            67.5 to "67.5° ENE",
            90.0 to "90.0° E",
            112.5 to "112.5° ESE",
            135.0 to "135.0° SE",
            157.5 to "157.5° SSE",
            180.0 to "180.0° S",
            202.5 to "202.5° SSW",
            225.0 to "225.0° SW",
            247.5 to "247.5° WSW",
            270.0 to "270.0° W",
            292.5 to "292.5° WNW",
            315.0 to "315.0° NW",
            337.5 to "337.5° NNW",
        )
        expected.forEach { (deg, text) ->
            assertEquals(text, deg.formatAzimuth(Locale.US))
        }
    }

    @Test
    fun formatAzimuth_wraps360AndHandlesNullOrNegativeAngles() {
        assertEquals("0.0° N", 359.96.formatAzimuth(Locale.US))
        assertEquals("270.0° W", (-90.0).formatAzimuth(Locale.US))
        assertEquals("178.4°", 178.42.formatAzimuth(Locale.US, includeDirection = false))
        assertNull((null as Double?).formatAzimuth(Locale.US))
        assertNull(Double.NaN.formatAzimuth(Locale.US))
    }

    @Test
    fun isCulminationActive_matchesDisplayedMinuteAndExactInstant() {
        // 13.148 h = 13:08:52.8 -> displays as 13:09
        val eventHour = 13.0 + (8.0 * 60.0 + 52.8) / 3600.0
        val atExactJump = ZonedDateTime.of(2026, 6, 21, 13, 8, 52, 0, ZoneOffset.UTC)
        val duringDisplayedMinute = ZonedDateTime.of(2026, 6, 21, 13, 9, 45, 0, ZoneOffset.UTC)
        val beforeWindow = ZonedDateTime.of(2026, 6, 21, 13, 8, 10, 0, ZoneOffset.UTC)
        val afterWindow = ZonedDateTime.of(2026, 6, 21, 13, 10, 0, 0, ZoneOffset.UTC)

        assertTrue(isCulminationActive(eventHour, atExactJump))
        assertTrue(isCulminationActive(eventHour, duringDisplayedMinute))
        assertFalse(isCulminationActive(eventHour, beforeWindow))
        assertFalse(isCulminationActive(eventHour, afterWindow))
        assertFalse(isCulminationActive(null, atExactJump))
    }

    @Test
    fun isCulminationActive_handlesMidnightBoundaryCleanly() {
        val midnightEvent = 23.0 + (59.0 * 60.0 + 50.0) / 3600.0 // 23:59:50 -> rounds to 00:00
        assertTrue(isCulminationActive(midnightEvent, ZonedDateTime.of(2026, 6, 21, 23, 59, 50, 0, ZoneOffset.UTC)))
        assertTrue(isCulminationActive(midnightEvent, ZonedDateTime.of(2026, 6, 22, 0, 0, 15, 0, ZoneOffset.UTC)))
        assertFalse(isCulminationActive(midnightEvent, ZonedDateTime.of(2026, 6, 22, 0, 1, 0, 0, ZoneOffset.UTC)))
    }
}
