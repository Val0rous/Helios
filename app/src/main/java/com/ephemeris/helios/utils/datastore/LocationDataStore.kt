package com.ephemeris.helios.utils.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ephemeris.helios.utils.Coordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a single instance of DataStore attached to the Context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class LocationDataStore(private val context: Context) {

    // 1. Define the keys to store each property
    private val LATITUDE_KEY = doublePreferencesKey("latitude")
    private val LONGITUDE_KEY = doublePreferencesKey("longitude")
    private val ALTITUDE_KEY = doublePreferencesKey("altitude")

    // 2. Expose a Flow to read the coordinates
    val coordinatesFlow: Flow<Coordinates> = context.dataStore.data
        .map { preferences ->
            // Read values, providing a default fallback (e.g., your 3.1, 11.99)
            val lat = preferences[LATITUDE_KEY] ?: 3.1
            val lon = preferences[LONGITUDE_KEY] ?: 11.99
            val alt = preferences[ALTITUDE_KEY] ?: 0.0
            Coordinates(lat, lon, alt)
        }

    // 3. Function to save new coordinates
    suspend fun saveCoordinates(coordinates: Coordinates) {
        context.dataStore.edit { preferences ->
            preferences[LATITUDE_KEY] = coordinates.latitude
            preferences[LONGITUDE_KEY] = coordinates.longitude
            preferences[ALTITUDE_KEY] = coordinates.altitude
        }
    }
}