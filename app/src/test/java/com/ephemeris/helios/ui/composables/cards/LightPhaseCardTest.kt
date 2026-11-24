package com.ephemeris.helios.ui.composables.cards

import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
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

    @Test
    fun resolveTwilightStageWindows_unifiesOvernightAndSupportsTripleIntervals() {
        // 1. Exact boundary between Nautical Dawn end (5.5) and Civil Dawn start (5.5): only Civil is active
        val exactBoundary = ZonedDateTime.of(2026, 6, 15, 5, 30, 0, 0, ZoneOffset.UTC)
        val civil = resolveTwilightStageWindows(
            kind = TwilightStageKind.CIVIL,
            phaseIntervals = null,
            dawnLower = 5.5,
            dawnUpper = 6.0,
            duskUpper = 20.0,
            duskLower = 20.5,
            morningDuration = 0.5,
            eveningDuration = 0.5,
            currentTime = exactBoundary,
        )
        val nautical = resolveTwilightStageWindows(
            kind = TwilightStageKind.NAUTICAL,
            phaseIntervals = null,
            dawnLower = 4.8,
            dawnUpper = 5.5,
            duskUpper = 20.5,
            duskLower = 21.2,
            morningDuration = 0.7,
            eveningDuration = 0.7,
            currentTime = exactBoundary,
        )
        assertEquals(2, civil.size)
        assertTrue(civil[0].isActive)
        assertFalse(nautical[0].isActive)

        // 2. Scandinavia midsummer: Nautical bottoms out above -12° -> 1 unified overnight window (not split!)
        val overnightTime = ZonedDateTime.of(2026, 6, 21, 0, 30, 0, 0, ZoneOffset.UTC)
        val scandiNautical = resolveTwilightStageWindows(
            kind = TwilightStageKind.NAUTICAL,
            phaseIntervals = null,
            dawnLower = null,
            dawnUpper = 2.583,
            duskUpper = 23.417,
            duskLower = null,
            morningDuration = 1.583,
            eveningDuration = 1.583,
            currentTime = overnightTime,
        )
        assertEquals(1, scandiNautical.size)
        assertEquals(23.417, scandiNautical[0].startHour)
        assertEquals(2.583, scandiNautical[0].endHour)
        assertTrue(scandiNautical[0].isActive)

        // 3. Polar winter peak inside Nautical -> 1 unified midday window (not split!)
        val polarMidday = ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, ZoneOffset.UTC)
        val polarNautical = resolveTwilightStageWindows(
            kind = TwilightStageKind.NAUTICAL,
            phaseIntervals = null,
            dawnLower = 10.5,
            dawnUpper = null,
            duskUpper = null,
            duskLower = 13.5,
            morningDuration = 1.5,
            eveningDuration = 1.5,
            currentTime = polarMidday,
        )
        assertEquals(1, polarNautical.size)
        assertEquals(10.5, polarNautical[0].startHour)
        assertEquals(13.5, polarNautical[0].endHour)
        assertTrue(polarNautical[0].isActive)

        // 4. 3-interval day (midnight-split twilight) preserves all 3 windows with accurate active check
        val lateEvening = ZonedDateTime.of(2026, 5, 20, 23, 45, 0, 0, ZoneOffset.UTC)
        val tripleNautical = resolveTwilightStageWindows(
            kind = TwilightStageKind.NAUTICAL,
            phaseIntervals = listOf(
                SolarEphemeris.PhaseInterval(0.0, 0.75, 0.75, SolarEphemeris.PhaseDirection.SETTING),
                SolarEphemeris.PhaseInterval(2.417, 3.667, 1.25, SolarEphemeris.PhaseDirection.RISING),
                SolarEphemeris.PhaseInterval(23.5, 24.0, 0.5, SolarEphemeris.PhaseDirection.SETTING),
            ),
            dawnLower = null,
            dawnUpper = null,
            duskUpper = null,
            duskLower = null,
            morningDuration = 0.0,
            eveningDuration = 0.0,
            currentTime = lateEvening,
        )
        assertEquals(3, tripleNautical.size)
        assertFalse(tripleNautical[0].isActive)
        assertFalse(tripleNautical[1].isActive)
        assertTrue(tripleNautical[2].isActive)

        // 5. After-midnight Dusk + Morning Dawn orders Rising on left, Setting on right
        val ordered = resolveTwilightStageWindows(
            kind = TwilightStageKind.ASTRO,
            phaseIntervals = listOf(
                SolarEphemeris.PhaseInterval(0.1, 0.9, 0.8, SolarEphemeris.PhaseDirection.SETTING),
                SolarEphemeris.PhaseInterval(4.3, 5.1, 0.8, SolarEphemeris.PhaseDirection.RISING),
            ),
            dawnLower = null,
            dawnUpper = null,
            duskUpper = null,
            duskLower = null,
            morningDuration = 0.0,
            eveningDuration = 0.0,
            currentTime = lateEvening,
        )
        assertEquals(SolarEphemeris.PhaseDirection.RISING, ordered[0].direction)
        assertEquals(SolarEphemeris.PhaseDirection.SETTING, ordered[1].direction)
    }

    @Test
    fun resolvePlutoIntervals_matchesExactDisplayedIntervalForActiveHighlight() {
        val insideMorning = ZonedDateTime.of(2026, 6, 15, 5, 22, 0, 0, ZoneOffset.UTC)
        val outsideMorning = ZonedDateTime.of(2026, 6, 15, 5, 26, 0, 0, ZoneOffset.UTC)

        val active = resolvePlutoIntervals(
            dawnLower = 5.333,
            dawnUpper = 5.4,
            duskUpper = 20.917,
            duskLower = 20.983,
            morningDuration = 0.067,
            eveningDuration = 0.067,
            currentTime = insideMorning,
        )
        assertEquals(2, active.size)
        assertTrue(active[0].isActive)
        assertFalse(active[1].isActive)

        val inactive = resolvePlutoIntervals(
            dawnLower = 5.333,
            dawnUpper = 5.4,
            duskUpper = 20.917,
            duskLower = 20.983,
            morningDuration = 0.067,
            eveningDuration = 0.067,
            currentTime = outsideMorning,
        )
        assertFalse(inactive[0].isActive)
        assertFalse(inactive[1].isActive)
    }

    @Test
    fun solarEphemeris_bandScannerHandlesScandinaviaTripleIntervalsAndPolarExtremes() {
        // 1. Stockholm Midsummer (59.33°N, 18.07°E, Europe/Stockholm): Sun bottoms out at ~-7.2°
        // -> Civil has 2 intervals, Nautical merges into 1 continuous overnight interval, Astro is empty!
        val stockholm = Coordinates(59.3293, 18.0686, 0.0)
        val stockholmEvents = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, ZoneId.of("Europe/Stockholm")),
            stockholm,
        )
        assertEquals(2, stockholmEvents.civilIntervals.size)
        assertEquals(1, stockholmEvents.nauticalIntervals.size)
        assertTrue(stockholmEvents.nauticalIntervals.single().wrapsMidnight)
        assertTrue(stockholmEvents.astroIntervals.isEmpty())

        // 2. Bergen May 20 (60.39°N, 5.32°E, Europe/Oslo): solar midnight ~01:35 CEST, bottoms out ~-10°..-11°?
        // Test Bergen May 10 where solar midnight dips below -12° around ~01:35 CEST while Nautical starts before 24:00:
        val bergen = Coordinates(60.3913, 5.3221, 0.0)
        val bergenEvents = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 5, 10, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")),
            bergen,
        )
        assertEquals(3, bergenEvents.nauticalIntervals.size)
        assertEquals(1, bergenEvents.astroIntervals.size)
        assertEquals(2, bergenEvents.civilIntervals.size)

        // 3. Tromso Polar Winter Solstice (69.65°N, 18.96°E): Sun peaks around -3.1° ->
        // Civil is 1 continuous midday interval, Nautical & Astro are 2 intervals!
        val tromso = Coordinates(69.6492, 18.9553, 0.0)
        val tromsoWinter = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")),
            tromso,
        )
        assertEquals(1, tromsoWinter.civilIntervals.size)
        assertEquals(SolarEphemeris.PhaseDirection.CONTINUOUS, tromsoWinter.civilIntervals.single().direction)
        assertEquals(2, tromsoWinter.nauticalIntervals.size)
        assertEquals(2, tromsoWinter.astroIntervals.size)

        // 4. Tromso Midnight Sun (June 21): all 3 twilights empty
        val tromsoSummer = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, ZoneId.of("Europe/Oslo")),
            tromso,
        )
        assertTrue(tromsoSummer.civilIntervals.isEmpty())
        assertTrue(tromsoSummer.nauticalIntervals.isEmpty())
        assertTrue(tromsoSummer.astroIntervals.isEmpty())
        assertTrue(tromsoSummer.blueIntervals.isEmpty())

        // 5. Rome mid-latitude day: standard 2-way Rising + Setting for all 3 twilights & Pluto
        val rome = Coordinates(41.9028, 12.4964, 0.0)
        val romeEvents = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneId.of("Europe/Rome")),
            rome,
        )
        assertEquals(2, romeEvents.civilIntervals.size)
        assertEquals(2, romeEvents.nauticalIntervals.size)
        assertEquals(2, romeEvents.astroIntervals.size)
        assertEquals(2, romeEvents.plutoIntervals.size)
    }
}
