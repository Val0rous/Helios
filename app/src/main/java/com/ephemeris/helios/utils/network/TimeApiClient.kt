package com.ephemeris.helios.utils.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TimeApiClient {

    suspend fun getTimezoneForCoordinates(lat: Double, lon: Double): TimezoneResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://timeapi.io/api/v1/timezone/coordinate?latitude=$lat&longitude=$lon")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // 5 seconds timeout
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseString)

                    TimezoneResponse(
                        timezone = json.getString("timezone"),
                        currentUtcOffsetSeconds = json.getInt("current_utc_offset_seconds"),
                        standardUtcOffsetSeconds = json.getInt("standard_utc_offset_seconds"),
                        dstUtcOffsetSeconds = json.getInt("dst_utc_offset_seconds"),
                        hasDst = json.getBoolean("has_dst"),
                        dstOffsetSeconds = json.getInt("dst_offset_seconds"),
                        dstActive = json.getBoolean("dst_active"),
                        // Handle nulls safely for the DST string dates
                        dstFrom = json.optString("dst_from").takeIf { it != "null" && it.isNotEmpty() },
                        dstUntil = json.optString("dst_until").takeIf { it != "null" && it.isNotEmpty() },
                        localTime = json.getString("local_time"),
                        dayOfWeek = json.getString("day_of_week"),
                        utcTime = json.getString("utc_time"),
                        unixTimestamp = json.getLong("unix_timestamp")
                    )
                } else {
                    null // Handle non-200 responses safely
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null // Fail gracefully on network errors
            }
        }
    }
}