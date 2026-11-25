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
}
