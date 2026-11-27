package com.ephemeris.helios.utils.calc

import androidx.annotation.DrawableRes
import com.ephemeris.helios.R
import kotlin.math.pow

/**
 * Cloud layer / optical thickness categories governing radiative transfer when cloud cover > 0%.
 *
 * Transmittance parameters follow standard atmospheric cloud modification models
 * (Kasten-Czeplak broadband SW, Bais/Calbó erythemal UV CMF, and Mie cloud-deck CCT).
 */
enum class CloudType(
    val label: String,
    val layerLabel: String,
    val description: String,
    val overcastSwTransmittance: Double,
    val overcastUvTransmittance: Double,
    val overcastBeamTransmittance: Double,
    val overcastSkyKelvin: Double,
) {
    HIGH_THIN(
        label = "Thin",
        layerLabel = "High · Cirrus",
        description = "Wispy ice clouds · hazy sun & soft shadows",
        overcastSwTransmittance = 0.78,
        overcastUvTransmittance = 0.85,
        overcastBeamTransmittance = 0.45,
        overcastSkyKelvin = 7200.0,
    ),
    MID_MODERATE(
        label = "Medium",
        layerLabel = "Mid · Altostratus",
        description = "Typical cloud deck · bright diffuse sky",
        overcastSwTransmittance = 0.38,
        overcastUvTransmittance = 0.48,
        overcastBeamTransmittance = 0.04,
        overcastSkyKelvin = 6500.0,
    ),
    LOW_THICK(
        label = "Thick",
        layerLabel = "Low · Stratus",
        description = "Dense low overcast · direct shadows diffused",
        overcastSwTransmittance = 0.22,
        overcastUvTransmittance = 0.28,
        overcastBeamTransmittance = 0.0,
        overcastSkyKelvin = 6200.0,
    ),
    STORM_HEAVY(
        label = "Heavy",
        layerLabel = "Storm · Cumulonimbus",
        description = "Dark storm cover · heavy light attenuation",
        overcastSwTransmittance = 0.10,
        overcastUvTransmittance = 0.12,
        overcastBeamTransmittance = 0.0,
        overcastSkyKelvin = 6800.0,
    );

    companion object {
        fun fromName(name: String?): CloudType =
            entries.firstOrNull { it.name == name } ?: MID_MODERATE
    }
}

/**
 * Ground surface reflectance (broadband & UV albedo) affecting diffuse bounce, UV reflection,
 * ambient color temperature, and mesopic lunar landscape luminance (Purkinje shift).
 *
 * [DEFAULT] applies zero modification so clear-sky calculations match the baseline model identically.
 */
enum class GroundSurface(
    val label: String,
    val description: String,
    val shortwaveAlbedo: Double,
    val uvAlbedo: Double,
    val lunarReflectance: Double,
    val ambientMiredShift: Double,
    val hasFresnel: Boolean = false,
) {
    DEFAULT(
        label = "Default",
        description = "No ground surface modifier",
        shortwaveAlbedo = AtmosphericConditions.BASELINE_SW_ALBEDO,
        uvAlbedo = AtmosphericConditions.BASELINE_UV_ALBEDO,
        lunarReflectance = AtmosphericConditions.BASELINE_LUNAR_REFLECTANCE,
        ambientMiredShift = 0.0,
    ),
    WATER(
        label = "Water",
        description = "Low overhead · high grazing Fresnel glare",
        shortwaveAlbedo = 0.06,
        uvAlbedo = 0.09,
        lunarReflectance = 0.06,
        ambientMiredShift = -6.0,
        hasFresnel = true,
    ),
    GRASS(
        label = "Grass",
        description = "Absorbs UV · mild foliage bounce",
        shortwaveAlbedo = 0.22,
        uvAlbedo = 0.03,
        lunarReflectance = 0.20,
        ambientMiredShift = 2.0,
    ),
    CONCRETE(
        label = "Concrete",
        description = "Bright neutral urban pavement reflection",
        shortwaveAlbedo = 0.30,
        uvAlbedo = 0.12,
        lunarReflectance = 0.30,
        ambientMiredShift = 0.0,
    ),
    BEACH(
        label = "Beach",
        description = "High sand bounce & strong UV reflection",
        shortwaveAlbedo = 0.38,
        uvAlbedo = 0.17,
        lunarReflectance = 0.38,
        ambientMiredShift = 10.0,
    ),
    SNOW(
        label = "Snow",
        description = "Extreme albedo & strong UV amplification",
        shortwaveAlbedo = 0.82,
        uvAlbedo = 0.80,
        lunarReflectance = 0.82,
        ambientMiredShift = -10.0,
    );

    companion object {
        fun fromName(name: String?): GroundSurface =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * User-selected atmospheric & ground conditions modifying radiative/optical metrics
 * (irradiance, UV index, illuminance, color temperature, and shadows) for both Sun and Moon.
 */
data class AtmosphericConditions(
    val cloudCoverPercent: Int = 0,
    val cloudType: CloudType = CloudType.MID_MODERATE,
    val groundSurface: GroundSurface = GroundSurface.DEFAULT,
) {
    val clampedCloudCover: Int get() = cloudCoverPercent.coerceIn(0, 100)
    val cloudFraction: Double get() = clampedCloudCover / 100.0
    val hasClouds: Boolean get() = clampedCloudCover > 0
    val hasCustomGround: Boolean get() = groundSurface != GroundSurface.DEFAULT
    val isDefault: Boolean get() = !hasClouds && !hasCustomGround

    val skyConditionLabel: String
        get() = when {
            clampedCloudCover == 0 -> "Clear"
            clampedCloudCover < 25 -> "Mostly Clear"
            clampedCloudCover < 60 -> "Partly Cloudy"
            clampedCloudCover < 85 -> "Mostly Cloudy"
            cloudType == CloudType.STORM_HEAVY -> "Stormy"
            else -> "Overcast"
        }

    @get:DrawableRes
    val weatherIconRes: Int
        get() = when {
            clampedCloudCover == 0 -> R.drawable.ic_clear_day
            clampedCloudCover < 60 -> R.drawable.ic_partly_cloudy_day
            clampedCloudCover < 85 -> R.drawable.ic_filter_drama
            cloudType == CloudType.STORM_HEAVY -> R.drawable.ic_foggy
            else -> R.drawable.ic_cloud
        }

    @get:DrawableRes
    val weatherIconFilledRes: Int
        get() = when {
            clampedCloudCover == 0 -> R.drawable.ic_clear_day_filled
            clampedCloudCover < 60 -> R.drawable.ic_partly_cloudy_day_filled
            clampedCloudCover < 85 -> R.drawable.ic_filter_drama_filled
            cloudType == CloudType.STORM_HEAVY -> R.drawable.ic_foggy_filled
            else -> R.drawable.ic_cloud_filled
        }

    /** Expected transmittance of direct-beam solar/lunar radiation (`1.0` when clear). */
    fun directBeamTransmittance(): Double {
        if (!hasClouds) return 1.0
        val c = cloudFraction
        return (1.0 - c) + c * cloudType.overcastBeamTransmittance
    }

    /** Nonlinear broadband shortwave global transmittance preserving broken-cloud scatter. */
    fun globalSwTransmittance(): Double {
        if (!hasClouds) return 1.0
        val c = cloudFraction
        return 1.0 - (1.0 - cloudType.overcastSwTransmittance) * c.pow(2.4)
    }

    /** Erythemal UV Cloud Modification Factor (`1.0` when clear). */
    fun uvCloudModificationFactor(): Double {
        if (!hasClouds) return 1.0
        val c = cloudFraction
        return 1.0 - (1.0 - cloudType.overcastUvTransmittance) * c.pow(2.2)
    }

    /** True when dense overcast effectively extinguishes crisp directional shadows. */
    fun isDirectShadowOccluded(): Boolean = hasClouds && directBeamTransmittance() < 0.02

    /** Diffuse daylight luminous efficacy (lm/W): shifts slightly higher under clouds due to NIR absorption. */
    fun diffuseLuminousEfficacy(): Double = BASE_DIFFUSE_LUMINOUS_EFFICACY + 5.0 * cloudFraction

    /** Effective shortwave surface albedo, including smooth Schlick-style Fresnel reflection on water. */
    fun effectiveSwAlbedo(smoothSinElev: Double): Double {
        if (!groundSurface.hasFresnel) return groundSurface.shortwaveAlbedo
        val grazing = (1.0 - smoothSinElev.coerceIn(0.0, 1.0)).pow(4)
        return groundSurface.shortwaveAlbedo + 0.30 * grazing
    }

    /** Effective erythemal UV surface albedo, including smooth grazing Fresnel reflection on water. */
    fun effectiveUvAlbedo(smoothSinElev: Double): Double {
        if (!groundSurface.hasFresnel) return groundSurface.uvAlbedo
        val grazing = (1.0 - smoothSinElev.coerceIn(0.0, 1.0)).pow(4)
        return groundSurface.uvAlbedo + 0.18 * grazing
    }

    /** Landscape reflectance for lunar Purkinje mesopic luminance (`0.18` when [GroundSurface.DEFAULT]). */
    fun effectiveLunarReflectance(smoothSinElev: Double): Double {
        if (!hasCustomGround) return BASELINE_LUNAR_REFLECTANCE
        if (!groundSurface.hasFresnel) return groundSurface.lunarReflectance
        val grazing = (1.0 - smoothSinElev.coerceIn(0.0, 1.0)).pow(4)
        return groundSurface.lunarReflectance + 0.30 * grazing
    }

    private fun skySwBackscatterAlbedo(): Double =
        0.08 + 0.52 * cloudFraction * (1.0 - cloudType.overcastSwTransmittance)

    private fun skyUvBackscatterAlbedo(): Double =
        0.30 + 0.45 * cloudFraction * (1.0 - cloudType.overcastUvTransmittance)

    /**
     * Extra diffuse shortwave flux (irradiance or illuminance) contributed by sky-ground multiple
     * reflection and surrounding environmental ground bounce. Strictly `0.0` for [GroundSurface.DEFAULT].
     */
    fun groundSwExtraDiffuse(
        downwellingBeforeGround: Double,
        smoothSinElev: Double,
    ): Double {
        if (!hasCustomGround || downwellingBeforeGround <= 0.0) return 0.0
        val rho = effectiveSwAlbedo(smoothSinElev)
        val alphaSky = skySwBackscatterAlbedo()
        val multipleBounce = (1.0 - BASELINE_SW_ALBEDO * alphaSky) / (1.0 - rho * alphaSky)
        val envBounce = 0.25 * (rho - BASELINE_SW_ALBEDO)
        return downwellingBeforeGround * ((multipleBounce - 1.0) + envBounce)
    }

    /**
     * Multiplicative UV surface modifier combining sky-ground UV backscattering and surrounding
     * environmental bounce. Strictly `1.0` for [GroundSurface.DEFAULT].
     */
    fun uvSurfaceMultiplier(smoothSinElev: Double): Double {
        if (!hasCustomGround) return 1.0
        val rhoUv = effectiveUvAlbedo(smoothSinElev)
        val alphaSkyUv = skyUvBackscatterAlbedo()
        val multipleBounce = (1.0 - BASELINE_UV_ALBEDO * alphaSkyUv) / (1.0 - rhoUv * alphaSkyUv)
        val envBounce = 1.0 + 0.40 * (rhoUv - BASELINE_UV_ALBEDO)
        return (multipleBounce * envBounce).coerceAtLeast(0.5)
    }

    /**
     * Adjusts diffuse/ambient skylight color temperature in mireds for Mie cloud-deck scattering
     * and subtle ground-bounce tint. Identity when [isDefault].
     */
    fun adjustDiffuseColorTemp(clearSkyDiffuseKelvin: Double): Double {
        if (clearSkyDiffuseKelvin.isNaN() || isDefault) return clearSkyDiffuseKelvin
        var mired = 1_000_000.0 / clearSkyDiffuseKelvin
        if (hasClouds) {
            val cloudWeight = cloudFraction.pow(1.2) * (1.0 - 0.55 * cloudType.overcastSwTransmittance)
            val overcastMired = 1_000_000.0 / cloudType.overcastSkyKelvin
            mired += cloudWeight * (overcastMired - mired)
        }
        if (hasCustomGround) {
            mired += groundSurface.ambientMiredShift
        }
        return 1_000_000.0 / mired.coerceIn(50.0, 600.0)
    }

    /**
     * Adjusts direct-beam color temperature for cloud veil filtering; returns `NaN` when the direct
     * disk is completely occluded by dense overcast. Identity when clear.
     */
    fun adjustDirectColorTemp(clearDirectKelvin: Double): Double {
        if (clearDirectKelvin.isNaN() || !hasClouds) return clearDirectKelvin
        val beamT = directBeamTransmittance()
        if (beamT < 0.005) return Double.NaN
        val veiledShare = (cloudFraction * cloudType.overcastBeamTransmittance) / beamT
        val blend = 0.28 * veiledShare
        if (blend <= 0.0) return clearDirectKelvin
        val clearMired = 1_000_000.0 / clearDirectKelvin
        val neutralMired = 1_000_000.0 / 6000.0
        return 1_000_000.0 / (clearMired + blend * (neutralMired - clearMired))
    }

    companion object {
        const val BASELINE_SW_ALBEDO = 0.15
        const val BASELINE_UV_ALBEDO = 0.05
        const val BASELINE_LUNAR_REFLECTANCE = 0.18
        const val BASE_DIFFUSE_LUMINOUS_EFFICACY = 115.0

        val DEFAULT = AtmosphericConditions()
    }
}
