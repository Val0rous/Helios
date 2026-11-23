package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MoonMetricsTest {

    private val greenwich = Coordinates(latitude = 51.4769, longitude = 0.0, altitude = 0.0)

    @Test
    fun meeusExample49a_february1977NewMoonMatchesPublishedJde() {
        // Jean Meeus, Astronomical Algorithms (2nd ed.), Example 49.a (p. 353):
        // k = -283 (New Moon of 1977 Feb 18, 03:37:42 TD) -> JDE = 2443192.65118
        val jde = MoonMetrics.calculateNewMoonJde(-283L)
        assertEquals(2443192.65118, jde, 0.0001)
    }

    @Test
    fun newMoonTransition_resetsAgeFlipsWaxingAndMatchesTrueElongation() {
        // November 2026 New Moon occurs on 2026-11-09 around 07:02 UTC (k = 332).
        val newMoonJde = MoonMetrics.calculateNewMoonJde(332L)
        val deltaT = calculateDeltaT(2026, 11)
        val newMoonJdUtc = newMoonJde - deltaT / 86400.0
        // Convert JD (UTC) to epoch seconds (1970-01-01T00:00:00Z = JD 2440587.5)
        val epochSeconds = ((newMoonJdUtc - 2440587.5) * 86400.0).toLong()
        val exactNewMoon = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC)

        val before = MoonMetrics.calculateMetrics(exactNewMoon.minusMinutes(5), greenwich)
        val after = MoonMetrics.calculateMetrics(exactNewMoon.plusMinutes(5), greenwich)

        // Age resets cleanly from ~29.x days to ~0.003 days across New Moon
        assertTrue("Expected age > 29.2 before New Moon, was ${before.ageDays}", before.ageDays > 29.2)
        assertTrue("Expected age < 0.01 after New Moon, was ${after.ageDays}", after.ageDays < 0.01)

        // Waxing flag flips right at New Moon (true elongation crosses 0° within ~1 min of Meeus Ch. 49)
        assertFalse("Expected waning just before New Moon", before.isWaxing)
        assertTrue("Expected waxing just after New Moon", after.isWaxing)

        // Both instants are inside the New Moon phase window with near-zero illumination
        assertEquals(MoonMetrics.LunarPhase.NEW_MOON, before.phase)
        assertEquals(MoonMetrics.LunarPhase.NEW_MOON, after.phase)
        assertTrue("Expected illumination < 0.3% at New Moon, was ${after.illuminationPercent}", after.illuminationPercent < 0.3)
    }

    @Test
    fun fullMoonAndQuarters_matchTrueElongationAndIllumination() {
        // Around 2026-11-24 Full Moon (~14.8–15.1 days after 2026-11-09 New Moon)
        val nearFull = ZonedDateTime.of(2026, 11, 24, 14, 53, 0, 0, ZoneOffset.UTC)
        val fullMetrics = MoonMetrics.calculateMetrics(nearFull, greenwich)

        assertEquals(MoonMetrics.LunarPhase.FULL_MOON, fullMetrics.phase)
        assertTrue("Expected illumination > 99.5% at Full Moon, was ${fullMetrics.illuminationPercent}", fullMetrics.illuminationPercent > 99.5)
        assertTrue("Expected age around 14.5..15.6 at Full Moon, was ${fullMetrics.ageDays}", fullMetrics.ageDays in 14.5..15.6)
    }
}
