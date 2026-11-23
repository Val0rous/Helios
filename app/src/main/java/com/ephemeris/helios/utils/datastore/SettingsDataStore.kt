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

enum class MoonIlluminanceMode(val label: String) {
    GLOBAL("Global"),
    DIRECT("Direct");

    fun next(): MoonIlluminanceMode = entries[(ordinal + 1) % entries.size]

    fun toIlluminanceMode(): IlluminanceMode = when (this) {
        GLOBAL -> IlluminanceMode.GLOBAL
        DIRECT -> IlluminanceMode.DIRECT
    }

    companion object {
        fun fromName(name: String?): MoonIlluminanceMode =
            entries.firstOrNull { it.name == name } ?: GLOBAL
    }
}

enum class ColorTempMode(val label: String) {
    DIRECT("Direct"),
    GLOBAL("Global"),
    DIFFUSE("Ambient");

    fun next(): ColorTempMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): ColorTempMode =
            entries.firstOrNull { it.name == name } ?: DIRECT
    }
}

enum class MoonColorTempMode(val label: String) {
    PURKINJE("Purkinje"),
    DIRECT("Direct");

    fun next(): MoonColorTempMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): MoonColorTempMode = when (name) {
            "PHYSICAL" -> DIRECT
            "PERCEIVED" -> PURKINJE
            else -> entries.firstOrNull { it.name == name } ?: PURKINJE
        }
    }
}

enum class DistanceUnit(val label: String) {
    AU("AU"),
    KM("km");

    fun next(): DistanceUnit = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): DistanceUnit =
            entries.firstOrNull { it.name == name } ?: AU
    }
}

enum class MoonDeclinationMode(val label: String) {
    GEOCENTRIC("Geo"),
    TOPOCENTRIC("Topo");

    fun next(): MoonDeclinationMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): MoonDeclinationMode =
            entries.firstOrNull { it.name == name } ?: GEOCENTRIC
    }
}

/**
 * Central container for user-configurable app settings.
 *
 * @property useHorizonDip When `true`, lowers the apparent sunrise/sunset/moonrise/moonset horizon
 *   by the geometric dip `0.032° * sqrt(altitude_m)` for elevated observers. Defaults to `false`
 *   (strict sea-level apparent horizon, `-0.833°` for the Sun).
 * @property irradianceMode Selected display mode for the Sun Irradiance card (defaults to [IrradianceMode.DIRECT]).
 * @property illuminanceMode Selected display mode for the Sun Illuminance card (defaults to [IlluminanceMode.GLOBAL]).
 * @property colorTempMode Selected display mode for the Sun Color Temp card (defaults to [ColorTempMode.DIRECT]).
 * @property moonIlluminanceMode Selected display mode for the Moon Illuminance card (defaults to [MoonIlluminanceMode.GLOBAL]).
 * @property moonColorTempMode Selected display mode for the Moon Color Temp card (defaults to [MoonColorTempMode.PURKINJE]).
 * @property moonDeclinationMode Selected display mode for the Moon Declination card (defaults to [MoonDeclinationMode.GEOCENTRIC]).
 * @property distanceUnit Selected display unit for the Sun Distance card (defaults to [DistanceUnit.AU]).
 */
data class AppSettings(
    val useHorizonDip: Boolean = false,
    val irradianceMode: IrradianceMode = IrradianceMode.DIRECT,
    val illuminanceMode: IlluminanceMode = IlluminanceMode.GLOBAL,
    val colorTempMode: ColorTempMode = ColorTempMode.DIRECT,
    val moonIlluminanceMode: MoonIlluminanceMode = MoonIlluminanceMode.GLOBAL,
    val moonColorTempMode: MoonColorTempMode = MoonColorTempMode.PURKINJE,
    val moonDeclinationMode: MoonDeclinationMode = MoonDeclinationMode.GEOCENTRIC,
    val distanceUnit: DistanceUnit = DistanceUnit.AU,
)

/**
 * DataStore-backed persistence layer for [AppSettings], sharing the app's `"settings"` DataStore.
 */
class SettingsDataStore(private val context: Context) {

    private val useHorizonDipKey = booleanPreferencesKey("use_horizon_dip")
    private val irradianceModeKey = stringPreferencesKey("irradiance_mode")
    private val illuminanceModeKey = stringPreferencesKey("illuminance_mode")
    private val colorTempModeKey = stringPreferencesKey("color_temp_mode")
    private val moonIlluminanceModeKey = stringPreferencesKey("moon_illuminance_mode")
    private val moonColorTempModeKey = stringPreferencesKey("moon_color_temp_mode")
    private val moonDeclinationModeKey = stringPreferencesKey("moon_declination_mode")
    private val distanceUnitKey = stringPreferencesKey("distance_unit")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .map { preferences ->
            AppSettings(
                useHorizonDip = preferences[useHorizonDipKey] ?: false,
                irradianceMode = IrradianceMode.fromName(preferences[irradianceModeKey]),
                illuminanceMode = IlluminanceMode.fromName(preferences[illuminanceModeKey]),
                colorTempMode = ColorTempMode.fromName(preferences[colorTempModeKey]),
                moonIlluminanceMode = MoonIlluminanceMode.fromName(preferences[moonIlluminanceModeKey]),
                moonColorTempMode = MoonColorTempMode.fromName(preferences[moonColorTempModeKey]),
                moonDeclinationMode = MoonDeclinationMode.fromName(preferences[moonDeclinationModeKey]),
                distanceUnit = DistanceUnit.fromName(preferences[distanceUnitKey]),
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

    suspend fun setColorTempMode(mode: ColorTempMode) {
        context.dataStore.edit { preferences ->
            preferences[colorTempModeKey] = mode.name
        }
    }

    suspend fun setMoonIlluminanceMode(mode: MoonIlluminanceMode) {
        context.dataStore.edit { preferences ->
            preferences[moonIlluminanceModeKey] = mode.name
        }
    }

    suspend fun setMoonColorTempMode(mode: MoonColorTempMode) {
        context.dataStore.edit { preferences ->
            preferences[moonColorTempModeKey] = mode.name
        }
    }

    suspend fun setMoonDeclinationMode(mode: MoonDeclinationMode) {
        context.dataStore.edit { preferences ->
            preferences[moonDeclinationModeKey] = mode.name
        }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.dataStore.edit { preferences ->
            preferences[distanceUnitKey] = unit.name
        }
    }
}
