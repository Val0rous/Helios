package com.ephemeris.helios.utils.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ephemeris.helios.utils.location.Coordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a single instance of DataStore attached to the Context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class LocationDataStore(private val context: Context) {

    // 1. Define the keys to store each property
    private val latitudeKey = doublePreferencesKey("latitude")
    private val longitudeKey = doublePreferencesKey("longitude")
    private val altitudeKey = doublePreferencesKey("altitude")

    // 2. Expose a Flow to read the coordinates
    val coordinatesFlow: Flow<Coordinates?> = context.dataStore.data
        .map { preferences ->
            // Read values, providing a default fallback (e.g., your 3.1, 11.99)
            val lat = preferences[latitudeKey]
            val lon = preferences[longitudeKey]
            val alt = preferences[altitudeKey]
            if (lat != null && lon != null && alt != null) {
                Coordinates(lat, lon, alt)
            } else null
        }

    // 3. Function to save new coordinates
    suspend fun saveCoordinates(coordinates: Coordinates) {
        context.dataStore.edit { preferences ->
            preferences[latitudeKey] = coordinates.latitude
            preferences[longitudeKey] = coordinates.longitude
            preferences[altitudeKey] = coordinates.altitude
        }
    }
}