package com.ephemeris.helios.utils.network

data class TimezoneResponse(
    val timezone: String,
    val currentUtcOffsetSeconds: Int,
    val standardUtcOffsetSeconds: Int,
    val dstUtcOffsetSeconds: Int,
    val hasDst: Boolean,
    val dstOffsetSeconds: Int,
    val dstActive: Boolean,
    val dstFrom: String?,
    val dstUntil: String?,
    val localTime: String,
    val dayOfWeek: String,
    val utcTime: String,
    val unixTimestamp: Long
)