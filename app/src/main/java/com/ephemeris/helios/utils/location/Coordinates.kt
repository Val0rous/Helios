package com.ephemeris.helios.utils.location

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val locationName: String? = null,
    val timezoneId: String? = null
)