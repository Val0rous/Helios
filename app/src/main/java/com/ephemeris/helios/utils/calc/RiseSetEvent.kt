package com.ephemeris.helios.utils.calc

/**
 * A single horizon crossing (rise or set) occurring within a local calendar day.
 *
 * In high-latitude / polar regions and on near-midnight lunar days, a body can cross the horizon
 * 0, 1, 2, or 3 times within a single calendar day (e.g., Set -> Rise -> Set or Rise -> Set -> Rise).
 *
 * @property timeHour Wall-clock decimal hour in `[0.0, 24.0)`.
 * @property azimuth True bearing in degrees `[0.0, 360.0)` at the moment of crossing.
 * @property isRise `true` for a rising crossing (sunrise / moonrise), `false` for a setting crossing.
 */
data class RiseSetEvent(
    val timeHour: Double,
    val azimuth: Double,
    val isRise: Boolean,
)
