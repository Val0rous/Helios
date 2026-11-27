package com.ephemeris.helios.utils.calc

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

/**
 * Nutation in ecliptic longitude ([deltaPsiDeg]) and obliquity ([deltaEpsDeg]), in degrees.
 */
internal data class Nutation(
    val deltaPsiDeg: Double,
    val deltaEpsDeg: Double,
)

/**
 * Full 63 periodic terms of the IAU 1980 Theory of Nutation (Meeus Chapter 22, Table 22.A;
 * NREL SPA Table A4.3).
 *
 * Row layout: `[cD, cM, cMPrime, cF, cOmega, psiA, psiB, epsC, epsD]`, where `psiA..epsD`
 * are in units of `0.0001"` (ten-thousandths of an arcsecond).
 */
private val NUTATION_TERMS = arrayOf(
    doubleArrayOf( 0.0,  0.0,  0.0,  0.0,  1.0, -171996.0, -174.2,  92025.0,  8.9),
    doubleArrayOf(-2.0,  0.0,  0.0,  2.0,  2.0,  -13187.0,   -1.6,   5736.0, -3.1),
    doubleArrayOf( 0.0,  0.0,  0.0,  2.0,  2.0,   -2274.0,   -0.2,    977.0, -0.5),
    doubleArrayOf( 0.0,  0.0,  0.0,  0.0,  2.0,    2062.0,    0.2,   -895.0,  0.5),
    doubleArrayOf( 0.0,  1.0,  0.0,  0.0,  0.0,    1426.0,   -3.4,     54.0, -0.1),
    doubleArrayOf( 0.0,  0.0,  1.0,  0.0,  0.0,     712.0,    0.1,     -7.0,  0.0),
    doubleArrayOf(-2.0,  1.0,  0.0,  2.0,  2.0,    -517.0,    1.2,    224.0, -0.6),
    doubleArrayOf( 0.0,  0.0,  0.0,  2.0,  1.0,    -386.0,   -0.4,    200.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  1.0,  2.0,  2.0,    -301.0,    0.0,    129.0, -0.1),
    doubleArrayOf(-2.0, -1.0,  0.0,  2.0,  2.0,     217.0,   -0.5,    -95.0,  0.3),
    doubleArrayOf(-2.0,  0.0,  1.0,  0.0,  0.0,    -158.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  0.0,  2.0,  1.0,     129.0,    0.1,    -70.0,  0.0),
    doubleArrayOf( 0.0,  0.0, -1.0,  2.0,  2.0,     123.0,    0.0,    -53.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  0.0,  0.0,  0.0,      63.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  1.0,  0.0,  1.0,      63.0,    0.1,    -33.0,  0.0),
    doubleArrayOf( 2.0,  0.0, -1.0,  2.0,  2.0,     -59.0,    0.0,     26.0,  0.0),
    doubleArrayOf( 0.0,  0.0, -1.0,  0.0,  1.0,     -58.0,   -0.1,     32.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  1.0,  2.0,  1.0,     -51.0,    0.0,     27.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  2.0,  0.0,  0.0,      48.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0, -2.0,  2.0,  1.0,      46.0,    0.0,    -24.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  0.0,  2.0,  2.0,     -38.0,    0.0,     16.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  2.0,  2.0,  2.0,     -31.0,    0.0,     13.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  2.0,  0.0,  0.0,      29.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  1.0,  2.0,  2.0,      29.0,    0.0,    -12.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  0.0,  2.0,  0.0,      26.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  0.0,  2.0,  0.0,     -22.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0, -1.0,  2.0,  1.0,      21.0,    0.0,    -10.0,  0.0),
    doubleArrayOf( 0.0,  2.0,  0.0,  0.0,  0.0,      17.0,   -0.1,      0.0,  0.0),
    doubleArrayOf( 2.0,  0.0, -1.0,  0.0,  1.0,      16.0,    0.0,     -8.0,  0.0),
    doubleArrayOf(-2.0,  2.0,  0.0,  2.0,  2.0,     -16.0,    0.1,      7.0,  0.0),
    doubleArrayOf( 0.0,  1.0,  0.0,  0.0,  1.0,     -15.0,    0.0,      9.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  1.0,  0.0,  1.0,     -13.0,    0.0,      7.0,  0.0),
    doubleArrayOf( 0.0, -1.0,  0.0,  0.0,  1.0,     -12.0,    0.0,      6.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  2.0, -2.0,  0.0,      11.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 2.0,  0.0, -1.0,  2.0,  1.0,     -10.0,    0.0,      5.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  1.0,  2.0,  2.0,      -8.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 0.0,  1.0,  0.0,  2.0,  2.0,       7.0,    0.0,     -3.0,  0.0),
    doubleArrayOf(-2.0,  1.0,  1.0,  0.0,  0.0,      -7.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0, -1.0,  0.0,  2.0,  2.0,      -7.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  0.0,  2.0,  1.0,      -7.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  1.0,  0.0,  0.0,       6.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  2.0,  2.0,  2.0,       6.0,    0.0,     -3.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  1.0,  2.0,  1.0,       6.0,    0.0,     -3.0,  0.0),
    doubleArrayOf( 2.0,  0.0, -2.0,  0.0,  1.0,      -6.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 2.0,  0.0,  0.0,  0.0,  1.0,      -6.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 0.0, -1.0,  1.0,  0.0,  0.0,       5.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0, -1.0,  0.0,  2.0,  1.0,      -5.0,    0.0,      3.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  0.0,  0.0,  1.0,      -5.0,    0.0,      3.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  2.0,  2.0,  1.0,      -5.0,    0.0,      3.0,  0.0),
    doubleArrayOf(-2.0,  0.0,  2.0,  0.0,  1.0,       4.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  1.0,  0.0,  2.0,  1.0,       4.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  1.0, -2.0,  0.0,       4.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-1.0,  0.0,  1.0,  0.0,  0.0,      -4.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-2.0,  1.0,  0.0,  0.0,  0.0,      -4.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 1.0,  0.0,  0.0,  0.0,  0.0,      -4.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  1.0,  2.0,  0.0,       3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0, -2.0,  2.0,  2.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf(-1.0, -1.0,  1.0,  0.0,  0.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  1.0,  1.0,  0.0,  0.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0, -1.0,  1.0,  2.0,  2.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 2.0, -1.0, -1.0,  2.0,  2.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 0.0,  0.0,  3.0,  2.0,  2.0,      -3.0,    0.0,      0.0,  0.0),
    doubleArrayOf( 2.0, -1.0,  0.0,  2.0,  2.0,      -3.0,    0.0,      0.0,  0.0),
)

/**
 * Evaluates full IAU 1980 Nutation (Meeus Chapter 22, Table 22.A) for Julian Century [t] (TT since J2000.0).
 * Accurate to `< 0.001"` in both longitude (`Δψ`) and obliquity (`Δε`).
 */
internal fun calculateNutation(t: Double): Nutation {
    val t2 = t * t
    val t3 = t2 * t

    val d = 297.85036 + 445267.111480 * t - 0.0019142 * t2 + t3 / 189474.0
    val m = 357.52772 + 35999.050340 * t - 0.0001603 * t2 - t3 / 300000.0
    val mPrime = 134.96298 + 477198.867398 * t + 0.0086972 * t2 + t3 / 56250.0
    val f = 93.27191 + 483202.017538 * t - 0.0036825 * t2 + t3 / 327270.0
    val omega = 125.04452 - 1934.136261 * t + 0.0020708 * t2 + t3 / 450000.0

    var sumPsi = 0.0
    var sumEps = 0.0
    for (term in NUTATION_TERMS) {
        val argRad = Math.toRadians(
            term[0] * d + term[1] * m + term[2] * mPrime + term[3] * f + term[4] * omega,
        )
        sumPsi += (term[5] + term[6] * t) * sin(argRad)
        val epsCoeff = term[7] + term[8] * t
        if (epsCoeff != 0.0) {
            sumEps += epsCoeff * cos(argRad)
        }
    }

    return Nutation(
        deltaPsiDeg = sumPsi / 36_000_000.0,
        deltaEpsDeg = sumEps / 36_000_000.0,
    )
}

/**
 * Evaluates the Mean Obliquity of the Ecliptic (`ε₀`, in degrees) using Laskar's 10th-order
 * polynomial (Meeus Chapter 22, Eq. 22.3; accurate to `0.01"` over 2,000 years).
 */
internal fun calculateMeanObliquityDeg(t: Double): Double {
    val u = t / 100.0
    val arcSec = 84381.448 + u * (
        -4680.93 + u * (
            -1.55 + u * (
                1999.25 + u * (
                    -51.38 + u * (
                        -249.67 + u * (
                            -39.05 + u * (
                                7.12 + u * (
                                    27.87 + u * (
                                        5.79 + u * 2.45
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    return arcSec / 3600.0
}

/**
 * Evaluates the True Obliquity of the Ecliptic (`ε = ε₀ + Δε`, in degrees) for Julian Century [t].
 */
internal fun calculateTrueObliquityDeg(t: Double, deltaEpsDeg: Double): Double =
    calculateMeanObliquityDeg(t) + deltaEpsDeg

/**
 * Calculates Delta T (Terrestrial Time - Universal Time) in seconds.
 * Uses the Espenak and Meeus (2006) polynomial approximations.
 */
internal fun calculateDeltaT(year: Int, month: Int): Double {
    // Fractional year for polynomial calculation
    val y = year + (month - 0.5) / 12.0

    return when {
        // Modern era (2005 - 2050)
        year in 2005..2050 -> {
            val t = y - 2000.0
            // Note: We use a slight hybrid correction here.
            // The strict Espenak formula is: 62.92 + 0.32217 * t + 0.005589 * t * t
            // But to account for the recent anomalous speed-up in the 2020s,
            // returning ~69.2 for the current decade is more accurate than the raw polynomial.
            if (year in 2020..2028) {
                69.2
            } else {
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
        }

        // Late 20th Century (1986 - 2005)
        year in 1986..<2005 -> {
            val t = y - 2000.0
            63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t + 0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t
        }

        // Space Age (1961 - 1986)
        year in 1961..<1986 -> {
            val t = y - 1975.0
            45.45 + 1.067 * t - 0.026 * t * t - 0.00181 * t * t * t
        }

        // World War II Era (1941 - 1961)
        year in 1941..<1961 -> {
            val t = y - 1950.0
            29.07 + 0.407 * t - 0.0041 * t * t + 0.000209 * t * t * t
        }

        // Early 20th Century (1920 - 1941)
        year in 1920..<1941 -> {
            val t = y - 1920.0
            21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t
        }

        // Turn of the Century (1900 - 1920)
        year in 1900..<1920 -> {
            val t = y - 1900.0
            -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * t * t * t * t
        }

        // Future Fallback (2050 - 2150)
        year > 2050 -> {
            val t = y - 2000.0
            // A generalized parabola for the distant future
            62.92 + 0.32217 * t + 0.005589 * t * t
        }

        // Distant Past Fallback (Before 1900)
        else -> {
            val t = (y - 2000.0) / 100.0
            // Standard long-term approximation
            -20.0 + 32.0 * t * t
        }
    }
}

/**
 * Converts a True Geometric Altitude (where the body mathematically is)
 * into an Apparent Visual Altitude (where the eye actually sees it).
 */
fun Double.applyAtmosphericRefraction(): Double {
    // Refraction math physically breaks down and explodes to infinity deep below the horizon.
    // In ephemeris software, we only apply dynamic refraction if the body is above -2.0 degrees.
    if (this < -2.0) {
        return this
    }

    // Sæmundsson’s Formula for True-to-Apparent refraction
    // Calculates the atmospheric bend in arcminutes
    val rArcMinutes = 1.02 / tan(Math.toRadians(this + 10.3 / (this + 5.11)))

    // Convert arcminutes to decimal degrees and add it to the physical altitude
    return this + (rArcMinutes / 60.0)
}

const val MAX_SHADOW_RATIO = 5000.0
private val MIN_SHADOW_ALT_DEG = Math.toDegrees(atan(1.0 / MAX_SHADOW_RATIO)) // ~0.01146°

/**
 * Computes physical shadow ratio (shadow length / object height) using atmospheric refraction
 * and partial-disk visibility between apparent rise/set ([horizonAltDeg]) and full disk clearance.
 *
 * Returns 0.0 when below [horizonAltDeg] and caps smoothly at [MAX_SHADOW_RATIO] (5,000:1)
 * right as the upper limb grazes the horizon.
 */
fun computeShadowRatio(
    geometricAltDeg: Double,
    horizonAltDeg: Double,
    semiDiameterDeg: Double,
): Double {
    if (geometricAltDeg <= horizonAltDeg) return 0.0

    val apparentCenterAlt = geometricAltDeg.applyAtmosphericRefraction()
    val effectiveAltDeg = if (apparentCenterAlt >= semiDiameterDeg) {
        apparentCenterAlt
    } else {
        val horizonApparentCenter = horizonAltDeg.applyAtmosphericRefraction()
        val span = (semiDiameterDeg - horizonApparentCenter).coerceAtLeast(1e-6)
        val fractionRisen = ((apparentCenterAlt - horizonApparentCenter) / span).coerceIn(0.0, 1.0)
        semiDiameterDeg * fractionRisen
    }

    val safeElevRad = Math.toRadians(max(MIN_SHADOW_ALT_DEG, effectiveAltDeg))
    return (1.0 / tan(safeElevRad)).coerceAtMost(MAX_SHADOW_RATIO)
}