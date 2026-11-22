package com.ephemeris.helios.utils.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class IrradianceMode(val label: String) {
    DIRECT("Direct"),
    GLOBAL("Ground"),
    DIFFUSE("Diffuse");

    fun next(): IrradianceMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): IrradianceMode =
            entries.firstOrNull { it.name == name } ?: DIRECT
    }
}

enum class IlluminanceMode(val label: String) {
    GLOBAL("Global"),
    DIRECT("Direct"),
    DIFFUSE("Ambient");

    fun next(): IlluminanceMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): IlluminanceMode =
            entries.firstOrNull { it.name == name } ?: GLOBAL
    }
}

/**
 * Central container for user-configurable app settings.
 *
 * @property useHorizonDip When `true`, lowers the apparent sunrise/sunset/moonrise/moonset horizon
 *   by the geometric dip `0.032° * sqrt(altitude_m)` for elevated observers. Defaults to `false`
 *   (strict sea-level apparent horizon, `-0.833°` for the Sun).
 * @property irradianceMode Selected display mode for the Irradiance card (defaults to [IrradianceMode.DIRECT]).
 * @property illuminanceMode Selected display mode for the Illuminance card (defaults to [IlluminanceMode.GLOBAL]).
 */
data class AppSettings(
    val useHorizonDip: Boolean = false,
    val irradianceMode: IrradianceMode = IrradianceMode.DIRECT,
    val illuminanceMode: IlluminanceMode = IlluminanceMode.GLOBAL,
)

/**
 * DataStore-backed persistence layer for [AppSettings], sharing the app's `"settings"` DataStore.
 */
class SettingsDataStore(private val context: Context) {

    private val useHorizonDipKey = booleanPreferencesKey("use_horizon_dip")
    private val irradianceModeKey = stringPreferencesKey("irradiance_mode")
    private val illuminanceModeKey = stringPreferencesKey("illuminance_mode")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .map { preferences ->
            AppSettings(
                useHorizonDip = preferences[useHorizonDipKey] ?: false,
                irradianceMode = IrradianceMode.fromName(preferences[irradianceModeKey]),
                illuminanceMode = IlluminanceMode.fromName(preferences[illuminanceModeKey]),
            )
        }

    suspend fun setUseHorizonDip(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[useHorizonDipKey] = enabled
        }
    }

    suspend fun setIrradianceMode(mode: IrradianceMode) {
        context.dataStore.edit { preferences ->
            preferences[irradianceModeKey] = mode.name
        }
    }

    suspend fun setIlluminanceMode(mode: IlluminanceMode) {
        context.dataStore.edit { preferences ->
            preferences[illuminanceModeKey] = mode.name
        }
    }
}
