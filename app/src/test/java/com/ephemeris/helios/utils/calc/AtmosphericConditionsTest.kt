package com.ephemeris.helios.utils.calc

import com.ephemeris.helios.R
import com.ephemeris.helios.utils.location.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.sin

class AtmosphericConditionsTest {

    private val rome = Coordinates(latitude = 41.9028, longitude = 12.4964, altitude = 37.0)

    @Test
    fun defaultConditions_areExactIdentityForSunAndMoon() {
        val sunBaseline = SunMetrics.calculateMetrics(sunElevationDeg = 55.0, observerAltitudeMeters = 120.0, distanceAu = 1.0, ozoneDU = 300.0)
        val sunDefault = SunMetrics.calculateMetrics(
            sunElevationDeg = 55.0,
            observerAltitudeMeters = 120.0,
            distanceAu = 1.0,
            ozoneDU = 300.0,
            conditions = AtmosphericConditions.DEFAULT,
        )
        assertEquals(sunBaseline, sunDefault)

        val fullMoonTime = ZonedDateTime.of(2026, 11, 24, 23, 0, 0, 0, ZoneOffset.UTC)
        val moonBaseline = MoonMetrics.calculateMetrics(fullMoonTime, rome)
        val moonDefault = MoonMetrics.calculateMetrics(fullMoonTime, rome, AtmosphericConditions.DEFAULT)
        assertEquals(moonBaseline, moonDefault)
    }

    @Test
    fun cloudCoverAndDensity_attenuateBeamGlobalUvAndShiftSkyColorTemp() {
        val clear = SunMetrics.calculateMetrics(60.0, 0.0, conditions = AtmosphericConditions.DEFAULT)
        val partly = SunMetrics.calculateMetrics(
            60.0,
            0.0,
            conditions = AtmosphericConditions(cloudCoverPercent = 50, cloudType = CloudType.MID_MODERATE),
        )
        val thinOvercast = SunMetrics.calculateMetrics(
            60.0,
            0.0,
            conditions = AtmosphericConditions(cloudCoverPercent = 100, cloudType = CloudType.HIGH_THIN),
        )
        val midOvercast = SunMetrics.calculateMetrics(
            60.0,
            0.0,
            conditions = AtmosphericConditions(cloudCoverPercent = 100, cloudType = CloudType.MID_MODERATE),
        )
        val thickOvercast = SunMetrics.calculateMetrics(
            60.0,
            0.0,
            conditions = AtmosphericConditions(cloudCoverPercent = 100, cloudType = CloudType.LOW_THICK),
        )
        val stormOvercast = SunMetrics.calculateMetrics(
            60.0,
            0.0,
            conditions = AtmosphericConditions(cloudCoverPercent = 100, cloudType = CloudType.STORM_HEAVY),
        )

        // Broken clouds cut direct beam roughly in half while boosting diffuse sky scattering
        assertTrue("Partly cloudy DNI should drop sharply", partly.irradiance < clear.irradiance * 0.6)
        assertTrue("Partly cloudy diffuse sky should brighten vs clear sky", partly.diffuseIrradiance > clear.diffuseIrradiance * 2.0)
        assertTrue("Partly cloudy global irradiance should drop moderately", partly.globalIrradiance < clear.globalIrradiance)

        // Thicker overcast decks monotonically reduce global irradiance, illuminance, and UV
        assertTrue(clear.globalIrradiance > thinOvercast.globalIrradiance)
        assertTrue(thinOvercast.globalIrradiance > midOvercast.globalIrradiance)
        assertTrue(midOvercast.globalIrradiance > thickOvercast.globalIrradiance)
        assertTrue(thickOvercast.globalIrradiance > stormOvercast.globalIrradiance)

        assertTrue(clear.uvIntensity > thinOvercast.uvIntensity)
        assertTrue(thinOvercast.uvIntensity > midOvercast.uvIntensity)
        assertTrue(midOvercast.uvIntensity > thickOvercast.uvIntensity)
        assertTrue(thickOvercast.uvIntensity > stormOvercast.uvIntensity)

        // Opaque low/storm overcast extinguishes direct beam & crisp shadows while keeping overcast global CCT
        assertEquals(0.0, thickOvercast.irradiance, 1e-6)
        assertEquals(0.0, thickOvercast.shadowRatio, 1e-6)
        assertTrue("Direct CCT should be NaN when disk is occluded", thickOvercast.colorTemp.isNaN())
        assertTrue("Overcast global CCT should stay valid ~6000-7000K, got ${thickOvercast.globalColorTemp}", thickOvercast.globalColorTemp in 6000.0..7000.0)
        assertTrue("Overcast sky should be less blue than clear Rayleigh sky", thickOvercast.diffuseColorTemp < clear.diffuseColorTemp)

        // Thin cirrus keeps hazy direct sun & shadows
        assertTrue(thinOvercast.irradiance > 0.0)
        assertTrue(thinOvercast.shadowRatio > 0.0)
        assertFalse(thinOvercast.colorTemp.isNaN())
    }

    @Test
    fun groundSurface_modifiesUvDiffuseGlobalAndNeverDirectBeam() {
        val elev = 60.0
        val sinElev = sin(Math.toRadians(elev))

        val defaultM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.DEFAULT))
        val waterM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.WATER))
        val grassM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.GRASS))
        val concreteM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.CONCRETE))
        val beachM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.BEACH))
        val snowM = SunMetrics.calculateMetrics(elev, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.SNOW))

        // Direct beam is strictly unaffected by ground reflectance
        listOf(waterM, grassM, concreteM, beachM, snowM).forEach { m ->
            assertEquals(defaultM.irradiance, m.irradiance, 1e-4)
            assertEquals(defaultM.directIlluminance, m.directIlluminance, 1e-3)
            // Exact energy conservation: GHI = DNI * sin(h) + DHI
            assertEquals(m.irradiance * sinElev + m.diffuseIrradiance, m.globalIrradiance, 1e-3)
        }

        // High-sun UV ordering: Grass < Default < Water < Concrete < Beach < Snow
        assertTrue(grassM.uvIntensity < defaultM.uvIntensity)
        assertTrue(defaultM.uvIntensity < waterM.uvIntensity)
        assertTrue(waterM.uvIntensity < concreteM.uvIntensity)
        assertTrue(concreteM.uvIntensity < beachM.uvIntensity)
        assertTrue(beachM.uvIntensity < snowM.uvIntensity)

        // Global illuminance ordering at 60°: Water (dark overhead) < Default < Grass < Concrete < Beach < Snow
        assertTrue(waterM.globalIlluminance < defaultM.globalIlluminance)
        assertTrue(defaultM.globalIlluminance < grassM.globalIlluminance)
        assertTrue(grassM.globalIlluminance < concreteM.globalIlluminance)
        assertTrue(concreteM.globalIlluminance < beachM.globalIlluminance)
        assertTrue(beachM.globalIlluminance < snowM.globalIlluminance)

        // Water Fresnel reflection increases dramatically at low sun angles vs overhead
        val waterLowSun = SunMetrics.calculateMetrics(8.0, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.WATER))
        val defaultLowSun = SunMetrics.calculateMetrics(8.0, 0.0, conditions = AtmosphericConditions(groundSurface = GroundSurface.DEFAULT))
        assertTrue(
            "Low-sun water grazing glare should exceed baseline diffuse illuminance",
            waterLowSun.diffuseIlluminance > defaultLowSun.diffuseIlluminance,
        )
    }

    @Test
    fun moonMetrics_respondToCloudsAndGroundSurface() {
        // Full Moon high in the sky over Rome
        val fullMoonNight = ZonedDateTime.of(2026, 11, 24, 23, 0, 0, 0, ZoneOffset.UTC)
        val clear = MoonMetrics.calculateMetrics(fullMoonNight, rome, AtmosphericConditions.DEFAULT)
        val cloudy = MoonMetrics.calculateMetrics(
            fullMoonNight,
            rome,
            AtmosphericConditions(cloudCoverPercent = 80, cloudType = CloudType.LOW_THICK),
        )
        val snow = MoonMetrics.calculateMetrics(
            fullMoonNight,
            rome,
            AtmosphericConditions(groundSurface = GroundSurface.SNOW),
        )

        assertTrue("Moon should be up for this test instant", clear.globalIlluminanceLux > 0.05)
        assertTrue("Clouds should dim lunar illuminance", cloudy.globalIlluminanceLux < clear.globalIlluminanceLux)
        assertTrue("Snow ground should brighten global lunar illuminance", snow.globalIlluminanceLux > clear.globalIlluminanceLux)
        assertTrue(
            "Snow's higher landscape reflectance should reduce scotopic Purkinje blue shift",
            snow.perceivedColorTempKelvin < clear.perceivedColorTempKelvin,
        )
    }

    @Test
    fun c1SmoothnessAcrossHorizonAndZero_preservedUnderCloudsAndWaterFresnel() {
        val cond = AtmosphericConditions(
            cloudCoverPercent = 65,
            cloudType = CloudType.MID_MODERATE,
            groundSurface = GroundSurface.WATER,
        )
        val eps = 0.01
        val mMinus = SunMetrics.calculateMetrics(-eps, 0.0, conditions = cond)
        val mZero = SunMetrics.calculateMetrics(0.0, 0.0, conditions = cond)
        val mPlus = SunMetrics.calculateMetrics(eps, 0.0, conditions = cond)

        val luxLeft = (mZero.globalIlluminance - mMinus.globalIlluminance) / eps
        val luxRight = (mPlus.globalIlluminance - mZero.globalIlluminance) / eps
        assertTrue(
            "Expected C1-smooth illuminance across 0° under clouds+water: left=$luxLeft, right=$luxRight",
            abs(luxRight - luxLeft) / luxRight < 0.02,
        )
    }

    @Test
    fun weatherIconsAndLabels_trackCloudCoverAndStormType() {
        assertEquals(R.drawable.ic_clear_day, AtmosphericConditions(0).weatherIconRes)
        assertEquals("Clear", AtmosphericConditions(0).skyConditionLabel)

        assertEquals(R.drawable.ic_partly_cloudy_day, AtmosphericConditions(15).weatherIconRes)
        assertEquals("Mostly Clear", AtmosphericConditions(15).skyConditionLabel)

        assertEquals(R.drawable.ic_partly_cloudy_day, AtmosphericConditions(45).weatherIconRes)
        assertEquals("Partly Cloudy", AtmosphericConditions(45).skyConditionLabel)

        assertEquals(R.drawable.ic_filter_drama, AtmosphericConditions(75).weatherIconRes)
        assertEquals("Mostly Cloudy", AtmosphericConditions(75).skyConditionLabel)

        assertEquals(R.drawable.ic_cloud, AtmosphericConditions(95, CloudType.LOW_THICK).weatherIconRes)
        assertEquals("Overcast", AtmosphericConditions(95, CloudType.LOW_THICK).skyConditionLabel)

        assertEquals(R.drawable.ic_foggy, AtmosphericConditions(95, CloudType.STORM_HEAVY).weatherIconRes)
        assertEquals("Stormy", AtmosphericConditions(95, CloudType.STORM_HEAVY).skyConditionLabel)
    }
}
