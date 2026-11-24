package com.ephemeris.helios.ui.composables.cards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

class LightPhaseCardTest {

    @Test
    fun formatDecimalHourRange_collapsesSameAmPmIn12HourLocale() {
        assertEquals("6:12" to "7:10 AM", formatDecimalHourRange(6.2, 7.167, Locale.US))
        assertEquals("7:40" to "8:38 PM", formatDecimalHourRange(19.667, 20.633, Locale.US))
        assertEquals("12:00" to "2:25 PM", formatDecimalHourRange(12.0, 14.417, Locale.US))
        assertEquals("12:05" to "1:30 AM", formatDecimalHourRange(0.083, 1.5, Locale.US))
    }

    @Test
    fun formatDecimalHourRange_preservesBothAmPmAcrossNoonOrMidnight() {
        // Polar winter interval straddling noon
        assertEquals("10:30 AM" to "12:10 PM", formatDecimalHourRange(10.5, 12.167, Locale.US))
        // Arctic summer overnight interval straddling midnight
        assertEquals("10:15 PM" to "2:45 AM", formatDecimalHourRange(22.25, 2.75, Locale.US))
        // High-latitude evening interval wrapping past 24.0
        assertEquals("11:30 PM" to "12:15 AM", formatDecimalHourRange(23.5, 24.25, Locale.US))
    }

    @Test
    fun formatDecimalHourRange_uses24HourFormatUnchanged() {
        assertEquals("06:12" to "07:10", formatDecimalHourRange(6.2, 7.167, Locale.ITALY))
        assertEquals("10:30" to "12:10", formatDecimalHourRange(10.5, 12.167, Locale.ITALY))
    }

    @Test
    fun resolveLightPhaseIntervals_handlesStandardPolarAndOvernightCases() {
        // Standard 2-interval day, morning active
        val morningTime = ZonedDateTime.of(2026, 6, 15, 6, 35, 0, 0, ZoneOffset.UTC)
        val standard = resolveLightPhaseIntervals(
            dawnLower = 6.2,
            dawnUpper = 7.167,
            duskUpper = 19.667,
            duskLower = 20.633,
            morningDuration = 0.967,
            eveningDuration = 0.967,
            currentTime = morningTime,
        )
        assertEquals(2, standard.size)
        assertTrue(standard[0].isActive)
        assertFalse(standard[1].isActive)

        // Scandinavian winter: peaks inside band -> 1 merged interval
        val winterNoon = ZonedDateTime.of(2026, 12, 21, 11, 45, 0, 0, ZoneOffset.UTC)
        val winter = resolveLightPhaseIntervals(
            dawnLower = 10.5,
            dawnUpper = null,
            duskUpper = null,
            duskLower = 12.167,
            morningDuration = 0.833,
            eveningDuration = 0.833,
            currentTime = winterNoon,
        )
        assertEquals(1, winter.size)
        assertTrue(winter[0].isActive)
        assertEquals(1.666, winter[0].durationHours, 1e-3)

        // Arctic summer: bottoms out inside band -> 1 merged overnight interval active after midnight
        val afterMidnight = ZonedDateTime.of(2026, 6, 21, 1, 15, 0, 0, ZoneOffset.UTC)
        val summer = resolveLightPhaseIntervals(
            dawnLower = null,
            dawnUpper = 2.75,
            duskUpper = 22.25,
            duskLower = null,
            morningDuration = 2.25,
            eveningDuration = 2.25,
            currentTime = afterMidnight,
        )
        assertEquals(1, summer.size)
        assertTrue(summer[0].isActive)
        assertEquals(4.5, summer[0].durationHours, 1e-3)

        // Polar night: 0 intervals
        val polarNight = resolveLightPhaseIntervals(
            dawnLower = null,
            dawnUpper = null,
            duskUpper = null,
            duskLower = null,
            morningDuration = 0.0,
            eveningDuration = 0.0,
            currentTime = winterNoon,
        )
        assertTrue(polarNight.isEmpty())
    }
}
