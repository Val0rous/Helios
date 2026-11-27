package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.utils.formatDuration
import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class RiseSetEventsTest {

    private val rome = Coordinates(latitude = 41.9028, longitude = 12.4964, altitude = 21.0)
    private val tromso = Coordinates(latitude = 69.6492, longitude = 18.9553, altitude = 10.0)
    private val svalbard = Coordinates(latitude = 78.2232, longitude = 15.6267, altitude = 10.0)

    @Test
    fun solarRiseSetEvents_standardSummerDay_hasTwoChronologicalCrossings() {
        val zone = ZoneId.of("Europe/Rome")
        val time = ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, zone)
        val events = SolarEphemeris.calculateDailyEvents(time, rome)

        assertEquals(2, events.riseSetEvents.size)
        val rise = events.riseSetEvents[0]
        val set = events.riseSetEvents[1]

        assertTrue(rise.isRise)
        assertFalse(set.isRise)
        assertTrue(rise.timeHour < set.timeHour)
        assertEquals(events.sunrise!!, rise.timeHour, 1e-2)
        assertEquals(events.sunset!!, set.timeHour, 1e-2)
        assertTrue("Expected NE sunrise azimuth, got ${rise.azimuth}", rise.azimuth in 50.0..70.0)
        assertTrue("Expected NW sunset azimuth, got ${set.azimuth}", set.azimuth in 290.0..310.0)
    }

    @Test
    fun solarRiseSetEvents_polarDayAndPolarNight_haveZeroCrossingsAndExactDuration() {
        val zone = ZoneId.of("Arctic/Longyearbyen")
        val summer = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 6, 21, 12, 0, 0, 0, zone),
            svalbard,
        )
        val winter = SolarEphemeris.calculateDailyEvents(
            ZonedDateTime.of(2026, 12, 21, 12, 0, 0, 0, zone),
            svalbard,
        )

        assertTrue(summer.riseSetEvents.isEmpty())
        assertEquals(24.0, summer.dayLength, 1e-6)
        assertEquals("24h", summer.dayLength.formatDuration(showSeconds = true))

        assertTrue(winter.riseSetEvents.isEmpty())
        assertEquals(0.0, winter.dayLength, 1e-6)
        assertEquals("0h", winter.dayLength.formatDuration(showSeconds = true))
    }

    @Test
    fun solarRiseSetEvents_arcticSummerTransition_capturesThreeCrossingDays() {
        // In Tromsø (CEST = UTC+2), solar midnight is ~00:45 local clock time.
        // Around mid-May (entering midnight sun) and late July (leaving midnight sun), the Sun
        // sets shortly after 00:00, rises around 01:xx, and sets again before 24:00 (3 crossings).
        val zone = ZoneId.of("Europe/Oslo")
        var foundThreeEventDay = false

        var date = LocalDate.of(2026, 5, 1)
        val endDate = LocalDate.of(2026, 8, 15)
        while (!date.isAfter(endDate)) {
            val events = SolarEphemeris.calculateDailyEvents(
                date.atTime(12, 0).atZone(zone),
                tromso,
            )
            if (events.riseSetEvents.size == 3) {
                foundThreeEventDay = true
                val (e0, e1, e2) = events.riseSetEvents
                assertTrue("Crossings must be chronological on $date", e0.timeHour < e1.timeHour && e1.timeHour < e2.timeHour)
                assertTrue("Crossings must alternate rise/set on $date", e0.isRise != e1.isRise && e1.isRise != e2.isRise)
                assertTrue(e0.azimuth in 0.0..360.0 && e1.azimuth in 0.0..360.0 && e2.azimuth in 0.0..360.0)
                // For Set -> Rise -> Set, daylight is [0, e0] + [e1, e2]
                if (!e0.isRise) {
                    val expected = e0.timeHour + (e2.timeHour - e1.timeHour)
                    assertEquals("3-event daylight mismatch on $date", expected, events.dayLength, 2e-2)
                }
            }
            date = date.plusDays(1)
        }

        assertTrue("Expected at least one 3-crossing solar day in Tromsø summer", foundThreeEventDay)
    }

    @Test
    fun lunarRiseSetEvents_midLatitudeAndArctic_coverZeroOneTwoAndThreeCrossings() {
        val romeZone = ZoneId.of("Europe/Rome")
        var sawOneCrossing = false
        var sawTwoCrossingsSetFirst = false

        for (dayOffset in 0L..35L) {
            val dt = LocalDate.of(2026, 6, 1).plusDays(dayOffset).atTime(12, 0).atZone(romeZone)
            val lunar = LunarEphemeris.calculateDailyEvents(dt, rome)
            val list = lunar.riseSetEvents
            for (i in 0 until list.lastIndex) {
                assertTrue("Lunar crossings must be chronological", list[i].timeHour < list[i + 1].timeHour)
                assertTrue("Lunar crossings must alternate rise/set", list[i].isRise != list[i + 1].isRise)
            }
            if (list.size == 1) sawOneCrossing = true
            if (list.size == 2 && !list[0].isRise && list[1].isRise) sawTwoCrossingsSetFirst = true
        }

        assertTrue("Expected a single-crossing lunar day in Rome over a synodic month", sawOneCrossing)
        assertTrue("Expected a Set->Rise 2-crossing lunar day in Rome", sawTwoCrossingsSetFirst)

        // Arctic Svalbard over a full tropical month should show 0-crossing (up/down all day)
        // and verify chronological alternation on all days (including any 3-crossing days).
        val arcticZone = ZoneId.of("Arctic/Longyearbyen")
        var sawUpAllDay = false
        var sawDownAllDay = false
        var maxEventsInDay = 0
        for (dayOffset in 0L..35L) {
            val dt = LocalDate.of(2026, 6, 1).plusDays(dayOffset).atTime(12, 0).atZone(arcticZone)
            val lunar = LunarEphemeris.calculateDailyEvents(dt, svalbard)
            maxEventsInDay = maxOf(maxEventsInDay, lunar.riseSetEvents.size)
            if (lunar.isMoonUpAllDay) {
                sawUpAllDay = true
                assertTrue(lunar.riseSetEvents.isEmpty())
                assertEquals(24.0, lunar.moonDuration, 1e-6)
            }
            if (lunar.isMoonDownAllDay) {
                sawDownAllDay = true
                assertTrue(lunar.riseSetEvents.isEmpty())
                assertEquals(0.0, lunar.moonDuration, 1e-6)
            }
            for (i in 0 until lunar.riseSetEvents.lastIndex) {
                assertTrue(lunar.riseSetEvents[i].timeHour < lunar.riseSetEvents[i + 1].timeHour)
                assertTrue(lunar.riseSetEvents[i].isRise != lunar.riseSetEvents[i + 1].isRise)
            }
        }

        assertTrue("Expected Moon up all day in Svalbard", sawUpAllDay)
        assertTrue("Expected Moon down all day in Svalbard", sawDownAllDay)
        assertTrue("Expected lunar crossings in Svalbard", maxEventsInDay >= 2)
    }

    @Test
    fun formatDuration_withSeconds_handlesZeroTwentyFourAndRoundingBounds() {
        assertEquals("0h", 0.0.formatDuration(showSeconds = true))
        assertEquals("0h", (-0.1).formatDuration(showSeconds = true))
        assertEquals("0h", 0.0001.formatDuration(showSeconds = true))
        assertEquals("24h", 24.0.formatDuration(showSeconds = true))
        assertEquals("24h", 24.5.formatDuration(showSeconds = true))
        assertEquals("24h", 23.99995.formatDuration(showSeconds = true))
        assertEquals("45m 12s", (45.0 / 60.0 + 12.0 / 3600.0).formatDuration(showSeconds = true))
        assertEquals("12h 34m 18s", (12.0 + 34.0 / 60.0 + 18.0 / 3600.0).formatDuration(showSeconds = true))
        assertTrue(abs(12.5716667 - (12.0 + 34.0 / 60.0 + 18.0 / 3600.0)) < 1e-6)
    }

    @Test
    fun solarRiseSetEvents_mtErebusPolarGrazingAndShiftedTimezones_neverGlitchToMidnightSun() {
        val flagPairs = listOf(false to false, true to false, false to true, true to true)
        val zones = listOf(
            ZoneId.of("Antarctica/McMurdo"),
            ZoneId.of("Europe/Rome"),
            ZoneId.of("UTC"),
            java.time.ZoneOffset.ofHours(-5),
            java.time.ZoneOffset.ofHours(-10),
        )

        // 1. Ultra-fine sweep right across the August grazing touch at Mt. Erebus
        for (zone in zones) {
            for ((dip, instSd) in flagPairs) {
                val baseDate = if (dip) LocalDate.of(2026, 8, 11) else LocalDate.of(2026, 8, 17)
                val base = Coordinates(-77.5297, 167.1533, 3794.0, useHorizonDip = dip, useInstantaneousSemidiameter = instSd)
                val probe = SolarEphemeris.calculateDailyEvents(baseDate.atTime(12, 0).atZone(zone), base)
                val horizon = base.sunApparentHorizonAlt(probe.distanceAu)
                val exactGrazingLat = -77.5297 - (probe.solarNoonAltitude - horizon)

                for (k in -60..60) {
                    val lat = exactGrazingLat + k * 0.0001
                    val coords = base.copy(latitude = lat)
                    val ev = SolarEphemeris.calculateDailyEvents(baseDate.atTime(12, 0).atZone(zone), coords)
                    assertTrue(
                        "Mt. Erebus August grazing must never show Midnight Sun (zone=$zone dip=$dip inst=$instSd lat=$lat len=${ev.dayLength})",
                        ev.dayLength < 12.0,
                    )
                    if (ev.riseSetEvents.isEmpty()) {
                        assertEquals(
                            "Empty rise/set on polar-night grazing day must yield 0h (zone=$zone dip=$dip inst=$instSd lat=$lat)",
                            0.0,
                            ev.dayLength,
                            1e-6,
                        )
                    } else {
                        assertEquals("Grazing touch must emit rise+set pair, never unpaired crossing", 2, ev.riseSetEvents.size)
                        assertTrue(ev.riseSetEvents[0].isRise && !ev.riseSetEvents[1].isRise)
                    }
                }
            }
        }

        // 2. Full-year sweep at Mt. Erebus across shifted timezones (previously caused false 24h Midnight Sun
        //    on April/May polar-night transition days when exactSolarNoon fell on the previous calendar day).
        for (zone in zones) {
            for ((dip, instSd) in flagPairs) {
                val coords = Coordinates(-77.5297, 167.1533, 3794.0, useHorizonDip = dip, useInstantaneousSemidiameter = instSd)
                var date = LocalDate.of(2026, 1, 1)
                val end = LocalDate.of(2026, 12, 31)
                while (!date.isAfter(end)) {
                    val ev = SolarEphemeris.calculateDailyEvents(date.atTime(12, 0).atZone(zone), coords)
                    val h = coords.sunApparentHorizonAlt(ev.distanceAu)
                    if (ev.riseSetEvents.isEmpty()) {
                        if (ev.solarMidnightAltitude < h - 0.1) {
                            assertEquals(
                                "False Midnight Sun on $date (zone=$zone dip=$dip inst=$instSd len=${ev.dayLength})",
                                0.0,
                                ev.dayLength,
                                1e-6,
                            )
                        } else if (ev.solarMidnightAltitude > h + 0.1) {
                            val expectedDayHours = LocalDayTimeline(date, zone).durationHours
                            assertEquals(
                                "False Polar Night on $date (zone=$zone dip=$dip inst=$instSd len=${ev.dayLength})",
                                expectedDayHours,
                                ev.dayLength,
                                1e-6,
                            )
                        }
                    }
                    date = date.plusDays(1)
                }
            }
        }
    }

    @Test
    fun getExactDayLength_grazingNoonTouchWithMidnightBelowHorizon_returnsPolarNight() {
        // Noon barely exceeds threshold while solar midnight is deep below horizon -> Polar Night (0h), never 24h
        val grazingNight = SolarEphemeris.getExactDayLength(
            sunriseHours = null,
            sunsetHours = null,
            solarNoonAltitude = -0.832,
            sunriseSunsetAltThreshold = -0.833,
            dayDurationHours = 24.0,
            solarMidnightAltitude = -25.0,
        )
        assertEquals(0.0, grazingNight, 1e-9)

        // True Polar Day: both noon and midnight stay above the apparent horizon -> 24h
        val midnightSun = SolarEphemeris.getExactDayLength(
            sunriseHours = null,
            sunsetHours = null,
            solarNoonAltitude = 12.0,
            sunriseSunsetAltThreshold = -0.833,
            dayDurationHours = 24.0,
            solarMidnightAltitude = 1.5,
        )
        assertEquals(24.0, midnightSun, 1e-9)
    }
}
