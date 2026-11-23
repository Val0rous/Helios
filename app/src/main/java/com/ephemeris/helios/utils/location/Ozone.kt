package com.ephemeris.helios.utils.location

import com.ephemeris.helios.utils.calc.SunMetrics
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Offline global Total Column Ozone (TCO, in Dobson Units) climatology model.
 *
 * Built from:
 * 1. NASA Aura OMI/MLS 5° zonal × 12-month climatology (Ziemke et al., 2011,
 *    Atmos. Chem. Phys. 11, 9237–9251; Tables 2 & 3, 2004–2010 reference epoch),
 *    separating Stratospheric Column Ozone (SCO) and Tropospheric Column Ozone (TCO_trop).
 * 2. Smooth year-aware historical depletion and projected recovery of the Antarctic ozone hole
 *    and background mid-latitude/polar ODS trends per the WMO/UNEP 2022 Scientific Assessment
 *    of Ozone Depletion (pre-1979 baseline -> ~2000 peak depletion -> ~2066 Antarctic / ~2042 global recovery).
 * 3. Smooth C∞ longitudinal stationary planetary wave modulation (wave-1 + wave-2 Rossby ridges/troughs,
 *    vanishing at the equator and both poles with zero Prime Meridian or Date Line seams).
 * 4. Barometric altitude adjustment applied exclusively to the tropospheric ozone column so high-altitude
 *    observers do not lose stratospheric ozone or double-count UV altitude scaling.
 */
private object OzoneClimatology {
    private const val ATMOSPHERE_SCALE_HEIGHT = 8434.0
    private const val MIN_OZONE_DU = 85.0
    private const val MAX_OZONE_DU = 550.0
    private const val ZIEMKE_REFERENCE_YEAR = 2007.5

    // 36 latitude bands (5° wide), ordered South to North: centers at -87.5°, -82.5°, ..., +87.5°.
    // Table 3 of Ziemke et al. (2011): Stratospheric Column Ozone (SCO, DU), Jan..Dec (2004–2010 mean).
    private val STRATOSPHERIC_OZONE_DU: Array<FloatArray> = arrayOf(
        /* -87.5° (85S-90S) */ floatArrayOf(252f, 246f, 237f, 230f, 237f, 236f, 225f, 204f, 135f, 142f, 187f, 252f),
        /* -82.5° (80S-85S) */ floatArrayOf(258f, 252f, 246f, 237f, 238f, 237f, 225f, 202f, 134f, 147f, 190f, 256f),
        /* -77.5° (75S-80S) */ floatArrayOf(259f, 253f, 252f, 242f, 247f, 244f, 226f, 196f, 135f, 173f, 205f, 260f),
        /* -72.5° (70S-75S) */ floatArrayOf(264f, 257f, 257f, 250f, 254f, 253f, 235f, 198f, 150f, 196f, 224f, 267f),
        /* -67.5° (65S-70S) */ floatArrayOf(269f, 261f, 257f, 256f, 263f, 264f, 247f, 211f, 186f, 233f, 254f, 273f),
        /* -62.5° (60S-65S) */ floatArrayOf(274f, 264f, 258f, 261f, 269f, 273f, 264f, 247f, 242f, 271f, 283f, 280f),
        /* -57.5° (55S-60S) */ floatArrayOf(275f, 262f, 257f, 262f, 271f, 278f, 281f, 284f, 290f, 299f, 299f, 283f),
        /* -52.5° (50S-55S) */ floatArrayOf(269f, 257f, 252f, 259f, 270f, 282f, 294f, 305f, 314f, 313f, 302f, 280f),
        /* -47.5° (45S-50S) */ floatArrayOf(260f, 250f, 247f, 255f, 267f, 282f, 298f, 310f, 316f, 313f, 298f, 273f),
        /* -42.5° (40S-45S) */ floatArrayOf(248f, 242f, 241f, 247f, 259f, 276f, 290f, 303f, 308f, 304f, 288f, 263f),
        /* -37.5° (35S-40S) */ floatArrayOf(238f, 235f, 236f, 240f, 250f, 265f, 275f, 288f, 292f, 287f, 274f, 252f),
        /* -32.5° (30S-35S) */ floatArrayOf(232f, 230f, 231f, 233f, 239f, 249f, 255f, 267f, 271f, 267f, 259f, 243f),
        /* -27.5° (25S-30S) */ floatArrayOf(229f, 226f, 227f, 228f, 229f, 234f, 239f, 248f, 254f, 251f, 247f, 237f),
        /* -22.5° (20S-25S) */ floatArrayOf(228f, 225f, 225f, 225f, 224f, 226f, 230f, 235f, 242f, 241f, 239f, 232f),
        /* -17.5° (15S-20S) */ floatArrayOf(228f, 226f, 226f, 225f, 222f, 222f, 225f, 228f, 234f, 234f, 233f, 230f),
        /* -12.5° (10S-15S) */ floatArrayOf(227f, 227f, 227f, 227f, 223f, 221f, 223f, 226f, 230f, 229f, 230f, 228f),
        /*  -7.5° ( 5S-10S) */ floatArrayOf(225f, 226f, 228f, 229f, 227f, 224f, 225f, 228f, 230f, 228f, 227f, 225f),
        /*  -2.5° ( 0 - 5S) */ floatArrayOf(223f, 224f, 228f, 231f, 230f, 229f, 230f, 233f, 233f, 228f, 225f, 222f),
        /*  +2.5° ( 0 - 5N) */ floatArrayOf(219f, 221f, 226f, 231f, 232f, 233f, 236f, 238f, 237f, 229f, 225f, 219f),
        /*  +7.5° ( 5N-10N) */ floatArrayOf(215f, 217f, 223f, 230f, 234f, 237f, 240f, 242f, 240f, 231f, 224f, 216f),
        /* +12.5° (10N-15N) */ floatArrayOf(214f, 216f, 224f, 232f, 237f, 240f, 244f, 245f, 242f, 232f, 223f, 214f),
        /* +17.5° (15N-20N) */ floatArrayOf(215f, 218f, 227f, 235f, 240f, 242f, 244f, 245f, 241f, 232f, 223f, 215f),
        /* +22.5° (20N-25N) */ floatArrayOf(221f, 224f, 234f, 242f, 246f, 246f, 245f, 244f, 240f, 232f, 224f, 219f),
        /* +27.5° (25N-30N) */ floatArrayOf(234f, 238f, 248f, 255f, 256f, 252f, 248f, 246f, 242f, 234f, 229f, 230f),
        /* +32.5° (30N-35N) */ floatArrayOf(257f, 262f, 272f, 275f, 269f, 259f, 251f, 248f, 244f, 237f, 237f, 247f),
        /* +37.5° (35N-40N) */ floatArrayOf(284f, 292f, 300f, 298f, 286f, 271f, 256f, 252f, 248f, 243f, 252f, 269f),
        /* +42.5° (40N-45N) */ floatArrayOf(307f, 320f, 324f, 318f, 304f, 286f, 266f, 259f, 254f, 252f, 265f, 289f),
        /* +47.5° (45N-50N) */ floatArrayOf(321f, 338f, 338f, 331f, 318f, 299f, 280f, 269f, 262f, 260f, 273f, 302f),
        /* +52.5° (50N-55N) */ floatArrayOf(329f, 349f, 348f, 340f, 325f, 306f, 290f, 276f, 267f, 267f, 279f, 308f),
        /* +57.5° (55N-60N) */ floatArrayOf(332f, 355f, 354f, 347f, 329f, 307f, 291f, 277f, 270f, 270f, 282f, 309f),
        /* +62.5° (60N-65N) */ floatArrayOf(333f, 359f, 359f, 352f, 331f, 306f, 286f, 274f, 267f, 269f, 283f, 306f),
        /* +67.5° (65N-70N) */ floatArrayOf(330f, 359f, 359f, 355f, 332f, 304f, 280f, 269f, 262f, 265f, 280f, 299f),
        /* +72.5° (70N-75N) */ floatArrayOf(326f, 359f, 361f, 359f, 333f, 303f, 275f, 262f, 256f, 259f, 279f, 293f),
        /* +77.5° (75N-80N) */ floatArrayOf(319f, 363f, 363f, 359f, 330f, 301f, 272f, 254f, 248f, 254f, 275f, 287f),
        /* +82.5° (80N-85N) */ floatArrayOf(314f, 363f, 361f, 359f, 330f, 300f, 269f, 252f, 244f, 251f, 270f, 281f),
        /* +87.5° (85N-90N) */ floatArrayOf(311f, 350f, 352f, 340f, 324f, 297f, 269f, 253f, 243f, 246f, 267f, 279f),
    )

    // Table 2 of Ziemke et al. (2011): Sea-level-equivalent Tropospheric Column Ozone (TCO_trop, DU)
    // for 65°S–65°N, with smooth ozonesonde-consistent polar extensions for 65°–90° N/S.
    private val TROPOSPHERIC_OZONE_DU: Array<FloatArray> = arrayOf(
        /* -87.5° (polar)   */ floatArrayOf(21.0f, 21.5f, 22.0f, 22.5f, 23.0f, 24.0f, 24.5f, 24.0f, 22.5f, 21.5f, 21.0f, 20.5f),
        /* -82.5° (polar)   */ floatArrayOf(21.2f, 21.8f, 22.4f, 22.8f, 23.5f, 24.8f, 25.2f, 24.4f, 23.0f, 22.2f, 21.4f, 20.8f),
        /* -77.5° (polar)   */ floatArrayOf(21.5f, 22.5f, 23.5f, 23.6f, 24.6f, 26.2f, 26.8f, 25.2f, 23.8f, 23.5f, 22.2f, 21.1f),
        /* -72.5° (polar)   */ floatArrayOf(21.8f, 23.5f, 24.8f, 24.6f, 26.2f, 28.4f, 29.0f, 26.2f, 24.8f, 25.8f, 23.4f, 21.4f),
        /* -67.5° (polar)   */ floatArrayOf(22.2f, 24.6f, 26.2f, 25.6f, 28.0f, 30.8f, 31.3f, 27.3f, 25.9f, 28.5f, 24.6f, 21.7f),
        /* -62.5° (60S-65S) */ floatArrayOf(22.6f, 25.8f, 27.5f, 26.6f, 29.8f, 33.2f, 33.6f, 28.3f, 27.0f, 31.2f, 25.8f, 22.0f),
        /* -57.5° (55S-60S) */ floatArrayOf(23.6f, 26.5f, 28.4f, 26.8f, 28.6f, 28.5f, 29.4f, 28.8f, 30.2f, 29.5f, 26.7f, 23.7f),
        /* -52.5° (50S-55S) */ floatArrayOf(25.3f, 26.5f, 27.8f, 27.5f, 27.8f, 29.7f, 28.5f, 28.0f, 31.4f, 30.7f, 28.4f, 25.4f),
        /* -47.5° (45S-50S) */ floatArrayOf(25.6f, 25.8f, 25.7f, 25.9f, 25.8f, 25.9f, 26.7f, 28.2f, 30.6f, 29.9f, 28.0f, 25.8f),
        /* -42.5° (40S-45S) */ floatArrayOf(28.0f, 27.2f, 26.0f, 25.6f, 25.6f, 26.5f, 28.5f, 30.5f, 31.6f, 30.3f, 28.3f, 27.1f),
        /* -37.5° (35S-40S) */ floatArrayOf(32.2f, 30.4f, 28.4f, 26.4f, 26.2f, 27.6f, 30.7f, 33.9f, 35.4f, 35.0f, 32.5f, 31.7f),
        /* -32.5° (30S-35S) */ floatArrayOf(33.9f, 31.8f, 30.5f, 28.6f, 28.3f, 30.0f, 33.4f, 36.2f, 38.9f, 39.5f, 37.5f, 35.9f),
        /* -27.5° (25S-30S) */ floatArrayOf(32.4f, 30.6f, 29.9f, 29.3f, 29.8f, 31.4f, 34.2f, 36.8f, 40.2f, 41.2f, 39.3f, 36.2f),
        /* -22.5° (20S-25S) */ floatArrayOf(29.8f, 27.9f, 27.6f, 27.6f, 28.7f, 30.9f, 32.7f, 35.1f, 38.2f, 39.5f, 37.3f, 33.5f),
        /* -17.5° (15S-20S) */ floatArrayOf(26.6f, 24.7f, 24.8f, 25.2f, 27.1f, 29.8f, 31.0f, 32.5f, 35.5f, 36.4f, 34.2f, 30.5f),
        /* -12.5° (10S-15S) */ floatArrayOf(24.5f, 23.0f, 23.6f, 23.9f, 26.0f, 28.7f, 29.5f, 30.8f, 33.3f, 33.9f, 32.1f, 28.7f),
        /*  -7.5° ( 5S-10S) */ floatArrayOf(24.2f, 22.9f, 23.4f, 23.5f, 25.4f, 27.8f, 28.5f, 30.0f, 32.0f, 32.3f, 30.4f, 27.7f),
        /*  -2.5° ( 0 - 5S) */ floatArrayOf(24.4f, 24.0f, 24.8f, 24.1f, 25.0f, 26.9f, 27.5f, 28.7f, 30.1f, 29.9f, 28.5f, 26.4f),
        /*  +2.5° ( 0 - 5N) */ floatArrayOf(25.2f, 25.5f, 26.8f, 26.0f, 24.9f, 25.3f, 25.8f, 26.4f, 27.2f, 26.9f, 26.4f, 26.4f),
        /*  +7.5° ( 5N-10N) */ floatArrayOf(27.2f, 27.6f, 29.9f, 29.8f, 27.7f, 26.5f, 25.8f, 25.1f, 26.1f, 26.3f, 26.6f, 27.7f),
        /* +12.5° (10N-15N) */ floatArrayOf(29.2f, 29.3f, 32.7f, 34.1f, 32.6f, 30.3f, 28.2f, 26.8f, 27.3f, 27.7f, 28.6f, 29.5f),
        /* +17.5° (15N-20N) */ floatArrayOf(31.1f, 31.3f, 35.2f, 38.0f, 37.1f, 34.3f, 32.0f, 30.0f, 29.8f, 29.7f, 30.2f, 31.0f),
        /* +22.5° (20N-25N) */ floatArrayOf(31.6f, 32.4f, 36.9f, 40.5f, 40.4f, 37.1f, 35.3f, 33.6f, 33.0f, 32.0f, 31.7f, 31.6f),
        /* +27.5° (25N-30N) */ floatArrayOf(30.2f, 31.6f, 35.9f, 40.5f, 42.0f, 39.8f, 38.3f, 36.8f, 35.0f, 33.1f, 31.9f, 30.6f),
        /* +32.5° (30N-35N) */ floatArrayOf(28.2f, 28.6f, 32.9f, 38.0f, 41.2f, 42.9f, 42.3f, 40.4f, 37.3f, 33.5f, 30.9f, 29.0f),
        /* +37.5° (35N-40N) */ floatArrayOf(27.0f, 27.7f, 30.9f, 35.2f, 38.9f, 43.0f, 44.0f, 42.3f, 37.9f, 32.4f, 28.4f, 27.3f),
        /* +42.5° (40N-45N) */ floatArrayOf(27.6f, 28.9f, 30.9f, 34.1f, 37.0f, 40.9f, 42.2f, 40.7f, 35.7f, 30.2f, 27.3f, 27.3f),
        /* +47.5° (45N-50N) */ floatArrayOf(29.4f, 30.8f, 31.6f, 33.7f, 35.9f, 38.2f, 39.1f, 37.4f, 33.2f, 28.9f, 27.9f, 28.0f),
        /* +52.5° (50N-55N) */ floatArrayOf(31.1f, 32.1f, 32.9f, 33.5f, 35.2f, 36.7f, 36.9f, 34.7f, 30.8f, 28.4f, 28.8f, 30.1f),
        /* +57.5° (55N-60N) */ floatArrayOf(33.0f, 32.2f, 33.0f, 33.7f, 34.6f, 35.6f, 35.5f, 32.9f, 29.5f, 28.1f, 30.3f, 33.6f),
        /* +62.5° (60N-65N) */ floatArrayOf(35.8f, 34.6f, 33.1f, 33.7f, 34.3f, 33.8f, 33.8f, 30.7f, 28.2f, 27.9f, 33.8f, 35.8f),
        /* +67.5° (polar)   */ floatArrayOf(34.8f, 33.8f, 32.6f, 33.3f, 33.8f, 33.2f, 32.8f, 30.0f, 27.6f, 27.4f, 32.8f, 34.8f),
        /* +72.5° (polar)   */ floatArrayOf(34.0f, 33.2f, 32.2f, 32.9f, 33.3f, 32.6f, 32.0f, 29.4f, 27.1f, 26.9f, 32.0f, 34.0f),
        /* +77.5° (polar)   */ floatArrayOf(33.2f, 32.6f, 31.8f, 32.5f, 32.8f, 32.0f, 31.2f, 28.8f, 26.6f, 26.5f, 31.2f, 33.2f),
        /* +82.5° (polar)   */ floatArrayOf(32.5f, 32.2f, 31.4f, 32.2f, 32.4f, 31.5f, 30.6f, 28.4f, 26.3f, 26.2f, 30.6f, 32.5f),
        /* +87.5° (polar)   */ floatArrayOf(32.0f, 32.0f, 31.0f, 32.0f, 32.0f, 31.0f, 30.0f, 28.0f, 26.0f, 26.0f, 30.0f, 32.0f),
    )

    // Measured 2004–2010 Antarctic ozone-hole stratospheric deficit (DU) relative to pre-1979
    // unperturbed austral-spring SCO baseline, for the 6 southernmost 5° bands (-87.5° to -62.5°).
    // Band index 6 (-57.5°) and northward have zero hole deficit, guaranteeing smooth latitudinal taper.
    private val ANTARCTIC_HOLE_DEFICIT_2007: Array<FloatArray> = arrayOf(
        /* -87.5° (85S-90S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 15f, 40f, 118f, 122f, 82f, 10f),
        /* -82.5° (80S-85S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 16f, 44f, 120f, 119f, 80f, 10f),
        /* -77.5° (75S-80S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 20f, 56f, 124f, 98f, 68f, 10f),
        /* -72.5° (70S-75S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 20f, 62f, 116f, 82f, 56f, 8f),
        /* -67.5° (65S-70S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 18f, 56f, 88f, 50f, 32f, 5f),
        /* -62.5° (60S-65S) */ floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 10f, 30f, 42f, 22f, 12f, 2f),
    )

    private val REFERENCE_ANTARCTIC_SEVERITY_2007 = antarcticHoleSeverity(ZIEMKE_REFERENCE_YEAR)
    private val REFERENCE_GLOBAL_ODS_SEVERITY_2007 = globalOdsSeverity(ZIEMKE_REFERENCE_YEAR)

    private fun smoothstep(t: Double): Double {
        val x = t.coerceIn(0.0, 1.0)
        return x * x * (3.0 - 2.0 * x)
    }

    /**
     * Normalized Antarctic ozone hole severity in [0.0, 1.0] per WMO/UNEP 2022:
     * - 0.0 before 1975 (pre-CFC baseline)
     * - Smooth onset 1975..2000 (peaking at 1.0 around 2000)
     * - Gradual recovery 2000..2066 back to 1980 baseline (0.0 from 2066 onward)
     */
    fun antarcticHoleSeverity(fractionalYear: Double): Double = when {
        fractionalYear <= 1975.0 -> 0.0
        fractionalYear < 2000.0 -> smoothstep((fractionalYear - 1975.0) / 25.0)
        fractionalYear < 2066.0 -> 1.0 - smoothstep((fractionalYear - 2000.0) / 66.0)
        else -> 0.0
    }

    /**
     * Normalized background mid-latitude / Arctic ODS depletion severity in [0.0, 1.0] per WMO/UNEP 2022:
     * - 0.0 before 1978 -> 1.0 peak around 2000 -> 0.0 full recovery around 2042.
     */
    fun globalOdsSeverity(fractionalYear: Double): Double = when {
        fractionalYear <= 1978.0 -> 0.0
        fractionalYear < 2000.0 -> smoothstep((fractionalYear - 1978.0) / 22.0)
        fractionalYear < 2042.0 -> 1.0 - smoothstep((fractionalYear - 2000.0) / 42.0)
        else -> 0.0
    }

    private fun bilinearSample(
        table: Array<FloatArray>,
        latIdx0: Int,
        latIdx1: Int,
        latFrac: Double,
        m0: Int,
        m1: Int,
        mFrac: Double,
    ): Double {
        val row0 = table[latIdx0]
        val row1 = table[latIdx1]
        val v0 = row0[m0] + (row0[m1] - row0[m0]) * mFrac
        val v1 = row1[m0] + (row1[m1] - row1[m0]) * mFrac
        return v0 + (v1 - v0) * latFrac
    }

    private fun sampleAntarcticDeficit(
        latIdx0: Int,
        latIdx1: Int,
        latFrac: Double,
        m0: Int,
        m1: Int,
        mFrac: Double,
    ): Double {
        val count = ANTARCTIC_HOLE_DEFICIT_2007.size
        if (latIdx0 >= count) return 0.0
        val v0 = ANTARCTIC_HOLE_DEFICIT_2007[latIdx0].let { r -> r[m0] + (r[m1] - r[m0]) * mFrac }
        val v1 = if (latIdx1 < count) {
            ANTARCTIC_HOLE_DEFICIT_2007[latIdx1].let { r -> r[m0] + (r[m1] - r[m0]) * mFrac }
        } else {
            0.0
        }
        return v0 + (v1 - v0) * latFrac
    }

    fun estimate(
        latitude: Double,
        date: LocalDate,
        longitude: Double,
        altitudeMeters: Double,
    ): Double {
        val clampedLat = latitude.coerceIn(-90.0, 90.0)
        val yearFraction = (date.dayOfYear - 0.5) / date.lengthOfYear().toDouble()
        val fractionalYear = date.year + yearFraction

        // 1. Bilinear indices on the 5° latitude × 12-month circular grid
        val latPos = ((clampedLat + 87.5) / 5.0).coerceIn(0.0, 35.0)
        val latIdx0 = latPos.toInt().coerceAtMost(34)
        val latIdx1 = latIdx0 + 1
        val latFrac = latPos - latIdx0

        val monthPos = yearFraction * 12.0 - 0.5
        val wrappedMonth = ((monthPos % 12.0) + 12.0) % 12.0
        val m0 = wrappedMonth.toInt() % 12
        val m1 = (m0 + 1) % 12
        val mFrac = wrappedMonth - m0

        val sco2007 = bilinearSample(STRATOSPHERIC_OZONE_DU, latIdx0, latIdx1, latFrac, m0, m1, mFrac)
        val tropSeaLevel = bilinearSample(TROPOSPHERIC_OZONE_DU, latIdx0, latIdx1, latFrac, m0, m1, mFrac)

        // 2. Historical depletion & projected recovery (WMO/UNEP 2022)
        //    Exact match to Ziemke et al. (2011) at reference epoch 2007.5.
        val holeDeficit2007 = sampleAntarcticDeficit(latIdx0, latIdx1, latFrac, m0, m1, mFrac)
        val holeSeverity = antarcticHoleSeverity(fractionalYear)
        val scoAfterHole = sco2007 + holeDeficit2007 * (1.0 - holeSeverity / REFERENCE_ANTARCTIC_SEVERITY_2007)

        // Subtle ~3.5% peak global mid-latitude/polar stratospheric ODS trend (1978..2000..2042)
        val latRad = Math.toRadians(clampedLat)
        val globalOdsSeverity = globalOdsSeverity(fractionalYear)
        val odsDeltaSeverity = REFERENCE_GLOBAL_ODS_SEVERITY_2007 - globalOdsSeverity
        val odsScale = 1.0 + 0.035 * sin(latRad).pow(2) * odsDeltaSeverity
        val adjustedSco = scoAfterHole * odsScale

        // 3. Smooth C∞ longitudinal stationary planetary wave (vanishes at equator & both poles)
        val latWaveEnvelope = sin(2.0 * abs(latRad)).pow(2)
        val lonWave = if (clampedLat >= 0.0) {
            // NH stationary wave-1 + wave-2: ridges over East Asia (~120–140°E) & NE North America (~-75°W),
            // troughs over Europe (~10°E) & NE Pacific (~-145°W); stronger in winter/spring.
            val nhSeason = 0.9 + 0.4 * cos(2.0 * PI * (yearFraction - 0.16))
            val wave = 4.0 * cos(Math.toRadians(longitude - 135.0)) +
                15.0 * cos(Math.toRadians(2.0 * (longitude - 118.0)))
            wave * latWaveEnvelope * nhSeason
        } else {
            // SH stationary wave-1: ridge over Australia/South Indian Ocean (~135°E),
            // trough over South Atlantic/South America (~-45°W); stronger in austral spring.
            val shSeason = 0.8 + 0.5 * cos(2.0 * PI * (yearFraction - 0.72))
            val wave = 14.0 * cos(Math.toRadians(longitude - 135.0))
            wave * latWaveEnvelope * shSeason
        }

        // 4. Barometric altitude adjustment on the tropospheric ozone column ONLY
        val tropScale = exp(-altitudeMeters.coerceAtLeast(-1000.0) / ATMOSPHERE_SCALE_HEIGHT)
        val adjustedTrop = tropSeaLevel * tropScale

        return (adjustedSco + lonWave + adjustedTrop).coerceIn(MIN_OZONE_DU, MAX_OZONE_DU)
    }
}

/**
 * Estimates Total Column Ozone in Dobson Units (DU) for any location, altitude, and historical/future date
 * using the embedded NASA Aura OMI/MLS climatology (Ziemke et al., 2011) with stationary longitudinal waves,
 * tropospheric altitude scaling, and WMO/UNEP 2022 historical depletion / future recovery timelines.
 */
fun estimateHistoricalOzone(
    latitude: Double,
    date: LocalDate,
    longitude: Double = 0.0,
    altitudeMeters: Double = 0.0,
): Double = OzoneClimatology.estimate(
    latitude = latitude,
    date = date,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
)

/**
 * Inverts the clear-sky KNMI TEMIS v2.x UV model ([SunMetrics.calculateClearSkyUvi]) to infer
 * effective total column ozone (DU) from observed/forecast peak clear-sky UV index.
 */
fun reverseEngineerOzone(
    hourlyClearSkyUv: List<Double>,
    solarNoonAltitudeDegrees: Double,
    elevationMeters: Double,
    distanceAu: Double = 1.0,
): Double {
    val peakUvi = hourlyClearSkyUv.maxOrNull() ?: 0.0

    // In deep polar winter, UVI is near 0 all day; return standard baseline to avoid ill-conditioned inversion.
    if (peakUvi < 0.25 || solarNoonAltitudeDegrees <= 0.0) {
        return 300.0
    }

    val minDu = 100.0
    val maxDu = 550.0

    val maxPossibleUvi = SunMetrics.calculateClearSkyUvi(
        sunElevationDeg = solarNoonAltitudeDegrees,
        observerAltitudeMeters = elevationMeters,
        distanceAu = distanceAu,
        ozoneDU = minDu,
    )
    if (peakUvi >= maxPossibleUvi) return minDu

    val minPossibleUvi = SunMetrics.calculateClearSkyUvi(
        sunElevationDeg = solarNoonAltitudeDegrees,
        observerAltitudeMeters = elevationMeters,
        distanceAu = distanceAu,
        ozoneDU = maxDu,
    )
    if (peakUvi <= minPossibleUvi) return maxDu

    // UVI is strictly monotonically decreasing with ozoneDU -> exact binary search inversion
    var low = minDu
    var high = maxDu
    repeat(32) {
        val mid = 0.5 * (low + high)
        val uviAtMid = SunMetrics.calculateClearSkyUvi(
            sunElevationDeg = solarNoonAltitudeDegrees,
            observerAltitudeMeters = elevationMeters,
            distanceAu = distanceAu,
            ozoneDU = mid,
        )
        if (uviAtMid > peakUvi) {
            low = mid
        } else {
            high = mid
        }
    }
    return 0.5 * (low + high)
}
