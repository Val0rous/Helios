package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs

class LunarLunisticeTest {

    private val rome = Coordinates(latitude = 41.9028, longitude = 12.4964, altitude = 21.0)
    private val svalbard = Coordinates(latitude = 78.2232, longitude = 15.6267, altitude = 10.0)

    @Test
    fun majorStandstill2025_peaksNear28Point7DegreesInMarch() {
        val y2025 = LunarLunisticeEphemeris.getYearlyLunistices(2025, ZoneOffset.UTC, rome)

        assertEquals(2025, y2025.year)
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.MAJOR_STANDSTILL, y2025.cyclePhase)
        assertTrue(
            "Expected 2025 maxNorth >= 28.65°, got ${y2025.maxNorth.declinationDeg}",
            y2025.maxNorth.declinationDeg >= 28.65 && y2025.maxNorth.declinationDeg <= 28.80,
        )
        assertTrue(
            "Expected 2025 maxSouth <= -28.65°, got ${y2025.maxSouth.declinationDeg}",
            y2025.maxSouth.declinationDeg <= -28.65 && y2025.maxSouth.declinationDeg >= -28.80,
        )
        // Peak Major Standstill in 2025 occurs in March
        assertEquals(3, y2025.maxNorth.dateTime.monthValue)
        assertEquals(3, y2025.maxSouth.dateTime.monthValue)
        assertTrue(y2025.totalLunisticeCount in 26..28)
        assertEquals(y2025.totalLunisticeCount, y2025.allLunistices.size)
        assertTrue(y2025.allLunistices.contains(y2025.maxNorth))
        assertTrue(y2025.allLunistices.contains(y2025.maxSouth))
        assertTrue(y2025.allLunistices.contains(y2025.minNorth))
        assertTrue(y2025.allLunistices.contains(y2025.minSouth))
        // Strictly chronological, alternating North/South, all within 2025
        for (i in y2025.allLunistices.indices) {
            assertEquals(2025, y2025.allLunistices[i].dateTime.year)
            if (i > 0) {
                assertTrue(y2025.allLunistices[i].dateTime.isAfter(y2025.allLunistices[i - 1].dateTime))
                assertTrue(y2025.allLunistices[i].isNorth != y2025.allLunistices[i - 1].isNorth)
            }
        }
        assertNotNull(y2025.maxNorth.maxAltitudeDeg)
        assertNotNull(y2025.maxSouth.maxAltitudeDeg)
    }

    @Test
    fun minorStandstill2034_troughsNear18Point1Degrees() {
        val y2034 = LunarLunisticeEphemeris.getYearlyLunistices(2034, ZoneOffset.UTC, rome)

        assertEquals(2034, y2034.year)
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.MINOR_STANDSTILL, y2034.cyclePhase)
        assertTrue(
            "Expected 2034 minNorth near 18.1..18.4°, got ${y2034.minNorth.declinationDeg}",
            y2034.minNorth.declinationDeg in 18.0..18.5,
        )
        assertTrue(
            "Expected 2034 minSouth near -18.4..-18.1°, got ${y2034.minSouth.declinationDeg}",
            y2034.minSouth.declinationDeg in -18.5..-18.0,
        )
    }

    @Test
    fun cycleDriftAcross2026_2029_2039_2043_classifiedAccurately() {
        val y2026 = LunarLunisticeEphemeris.getYearlyLunistices(2026, ZoneId.of("Europe/Rome"), rome)
        val y2029 = LunarLunisticeEphemeris.getYearlyLunistices(2029, ZoneOffset.UTC, rome)
        val y2039 = LunarLunisticeEphemeris.getYearlyLunistices(2039, ZoneOffset.UTC, rome)
        val y2043 = LunarLunisticeEphemeris.getYearlyLunistices(2043, ZoneOffset.UTC, rome)

        // 2026: Post-Major Standstill narrowing from ~28.4° down toward ~27.1°
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.NEAR_MAJOR_NARROWING, y2026.cyclePhase)
        assertTrue(
            "Expected 2026 narrowing: start=${y2026.cycleStartAbsDecDeg}, end=${y2026.cycleEndAbsDecDeg}",
            y2026.cycleStartAbsDecDeg > y2026.cycleEndAbsDecDeg,
        )
        // Strictly within requested local calendar year
        assertEquals(2026, y2026.maxNorth.dateTime.year)
        assertEquals(2026, y2026.maxSouth.dateTime.year)
        assertEquals(2026, y2026.minNorth.dateTime.year)
        assertEquals(2026, y2026.minSouth.dateTime.year)

        // 2029: Mid-cycle narrowing (crosses solar solstice ±23.4°)
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.NARROWING, y2029.cyclePhase)
        assertTrue(y2029.cycleStartAbsDecDeg > y2029.cycleEndAbsDecDeg)

        // 2039: Mid-cycle widening (rising back toward 2043 Major Standstill)
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.WIDENING, y2039.cyclePhase)
        assertTrue(y2039.cycleEndAbsDecDeg > y2039.cycleStartAbsDecDeg)

        // 2043: Next Major Standstill (~18.6 years after 2025)
        assertEquals(LunarLunisticeEphemeris.StandstillCyclePhase.MAJOR_STANDSTILL, y2043.cyclePhase)
    }

    @Test
    fun arcticMaxSouthLunistice_staysBelowHorizonAndMatchesDeclination() {
        val y2025Svalbard = LunarLunisticeEphemeris.getYearlyLunistices(2025, ZoneOffset.UTC, svalbard)

        // At 78.2°N, a -28.7° South lunistice peaks around 90 - 78.2 - 28.7 = -16.9° (below horizon all day!)
        assertEquals(0.0, y2025Svalbard.maxSouth.moonUptimeHours, 1e-6)
        assertNotNull(y2025Svalbard.maxSouth.maxAltitudeDeg)
        assertTrue(
            "Expected Svalbard 2025 maxSouth culmination deeply below horizon, got ${y2025Svalbard.maxSouth.maxAltitudeDeg}",
            y2025Svalbard.maxSouth.maxAltitudeDeg!! < -15.0,
        )

        // And the refined lunistice instant matches LunarEphemeris.calculateDeclinationDeg to < 0.01°
        val directDec = LunarEphemeris.calculateDeclinationDeg(y2025Svalbard.maxNorth.dateTime)
        assertTrue(
            "Expected declination agreement within 0.01°, got ${y2025Svalbard.maxNorth.declinationDeg} vs $directDec",
            abs(y2025Svalbard.maxNorth.declinationDeg - directDec) < 0.01,
        )
    }
}
