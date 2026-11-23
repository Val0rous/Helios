package com.ephemeris.helios.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

object TimeApiClient {

    suspend fun getTimezoneForCoordinates(lat: Double, lon: Double): TimezoneResponse? {
        return withContext(Dispatchers.IO) {
            fetchFromTimeApiIo(lat, lon) ?: fetchFromOpenMeteo(lat, lon)
        }
    }

    private fun fetchFromTimeApiIo(lat: Double, lon: Double): TimezoneResponse? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = String.format(
                Locale.US,
                "https://timeapi.io/api/v1/timezone/coordinate?latitude=%.6f&longitude=%.6f",
                lat,
                lon,
            )
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val tzId = json.optString("timezone").takeIf { it.isNotBlank() } ?: return null
                // Validate that Android's ZoneId database recognizes this timezone ID
                ZoneId.of(tzId)

                TimezoneResponse(
                    timezone = tzId,
                    currentUtcOffsetSeconds = json.optInt("current_utc_offset_seconds", 0),
                    standardUtcOffsetSeconds = json.optInt("standard_utc_offset_seconds", 0),
                    dstUtcOffsetSeconds = json.optInt("dst_utc_offset_seconds", 0),
                    hasDst = json.optBoolean("has_dst", false),
                    dstOffsetSeconds = json.optInt("dst_offset_seconds", 0),
                    dstActive = json.optBoolean("dst_active", false),
                    dstFrom = json.optString("dst_from").takeIf { it != "null" && it.isNotEmpty() },
                    dstUntil = json.optString("dst_until").takeIf { it != "null" && it.isNotEmpty() },
                    localTime = json.optString("local_time", ""),
                    dayOfWeek = json.optString("day_of_week", ""),
                    utcTime = json.optString("utc_time", ""),
                    unixTimestamp = json.optLong("unix_timestamp", Instant.now().epochSecond),
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Free, zero-key fallback using Open-Meteo's IANA timezone lookup.
     */
    private fun fetchFromOpenMeteo(lat: Double, lon: Double): TimezoneResponse? {
        var connection: HttpURLConnection? = null
        return try {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f&timezone=auto&forecast_days=1",
                lat,
                lon,
            )
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseString)
                val tzId = json.optString("timezone").takeIf { it.isNotBlank() } ?: return null
                val zoneId = ZoneId.of(tzId)
                val now = Instant.now()
                val rules = zoneId.rules
                val currentOffsetSec = rules.getOffset(now).totalSeconds
                val standardOffsetSec = rules.getStandardOffset(now).totalSeconds
                val dstSavingsSec = rules.getDaylightSavings(now).seconds.toInt()
                val dstActive = rules.isDaylightSavings(now)
                val hasDst = !rules.isFixedOffset
                val localZdt = ZonedDateTime.ofInstant(now, zoneId)

                TimezoneResponse(
                    timezone = tzId,
                    currentUtcOffsetSeconds = currentOffsetSec,
                    standardUtcOffsetSeconds = standardOffsetSec,
                    dstUtcOffsetSeconds = standardOffsetSec + dstSavingsSec,
                    hasDst = hasDst,
                    dstOffsetSeconds = dstSavingsSec,
                    dstActive = dstActive,
                    dstFrom = null,
                    dstUntil = null,
                    localTime = localZdt.toString(),
                    dayOfWeek = localZdt.dayOfWeek.name,
                    utcTime = now.toString(),
                    unixTimestamp = now.epochSecond,
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}
