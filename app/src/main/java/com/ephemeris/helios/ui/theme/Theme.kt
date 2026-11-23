package com.ephemeris.helios.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.utilities.Hct

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Standardized alpha constants separating chart area fills from non-fill chart chrome
 * (icons, lines, labels, and gridlines).
 *
 * Base colors in [CustomColorScheme] remain 100% opaque (`1.0f`) by default; translucent
 * chart fills are derived automatically via [CustomColorScheme.fills] using these tiers.
 */
object ChartAlphas {
    // --- Chart Fills ---
    fun fillSky(isDark: Boolean): Float = if (isDark) 0.65f else 0.60f
    fun fillMetric(isDark: Boolean): Float = if (isDark) 0.65f else 0.50f
    fun fillElapsed(isDark: Boolean): Float = if (isDark) 0.30f else 0.35f

    // --- Non-fill Chrome (Icons, Labels, Grids) ---
    const val ICON_BELOW_HORIZON: Float = 0.50f
    fun label(isDark: Boolean): Float = if (isDark) 0.75f else 0.70f
    const val GRID_HORIZONTAL: Float = 0.30f
    const val GRID_VERTICAL: Float = 0.15f
}

/**
 * Translucent chart fill variants automatically derived from the opaque base colors in
 * [CustomColorScheme] using [ChartAlphas].
 */
@Immutable
data class ChartFillColors(
    val dayBackground: Color,
    val nightBackground: Color,
    val civilTwilight: Color,
    val nauticalTwilight: Color,
    val astronomicalTwilight: Color,
    val elapsedDay: Color,
    val elapsedNight: Color,
    val elapsedMoon: Color,
    val elapsedOther: Color,
    val moonBackground: Color,

    val amZenith: Color,
    val amHorizon: Color,
    val shadowShort: Color,
    val shadowLong: Color,
    val luxBright: Color,
    val luxDim: Color,
    val irrLow: Color,
    val irrMid: Color,
    val irrHigh: Color,

    val uvDarkGreen: Color,
    val uvGreen: Color,
    val uvYellow: Color,
    val uvAmber: Color,
    val uvOrange: Color,
    val uvRed: Color,
    val uvDarkRed: Color,
    val uvPurple: Color,

    val ct5500: Color,
    val ct4000: Color,
    val ct3000: Color,
    val ct2000: Color,
)

@Immutable
data class CustomColorScheme(
    val dayBackground: Color,
    val nightBackground: Color,
    val civilTwilight: Color,
    val nauticalTwilight: Color,
    val astronomicalTwilight: Color,
    val elapsedDay: Color,
    val elapsedNight: Color,
    val elapsedMoon: Color,
    val elapsedOther: Color,
    val moonBackground: Color,
    val sun: Color,
    val sunBorder: Color,
    val sunPath: Color,
    val chartCurve: Color,
    val chartLabel: Color,
    val dropLine: Color,
    val moon: Color,
    val moonUnlit: Color,
    val moonBorder: Color,
    val moonPath: Color,
    val nightPrimary: Color,
    val nightOnPrimary: Color,
    val nightPrimaryContainer: Color,
    val nightOnPrimaryContainer: Color,

    // --- Gradient Theme Colors (stored opaque 1.0f by default) ---
    // Air Mass (Clear Sky to Hazy Horizon)
    val amZenith: Color,
    val amHorizon: Color,
    val amWave: Color,
    // Shadows (Light/Short to Dark/Long)
    val shadowShort: Color,
    val shadowLong: Color,
    val shadowDial: Color,
    // Illuminance (Blinding Light to Dim)
    val luxBright: Color,
    val luxDim: Color,
    val luxWave: Color,
    // Irradiance Heat Map (Warm to Hot)
    val irrLow: Color,
    val irrMid: Color,
    val irrHigh: Color,
    val irrDial: Color,
    // Directional warm/cool tile fills (Declination & Velocity)
    val warmWave: Color,
    val coolWave: Color,

    // UV Index slices
    val uvDarkGreen: Color,
    val uvGreen: Color,
    val uvYellow: Color,
    val uvAmber: Color,
    val uvOrange: Color,
    val uvRed: Color,
    val uvDarkRed: Color,
    val uvPurple: Color,

    // Color Temperature gradient
    val ct5500: Color,
    val ct4000: Color,
    val ct3000: Color,
    val ct2000: Color,

    // Precomputed translucent chart fills (standardized via ChartAlphas)
    val fills: ChartFillColors,
)

/**
 * Derives 100% opaque base colors for both Light and Dark themes from their Light-mode seed colors
 * using [MaterialColorEngine], and precomputes standardized translucent [CustomColorScheme.fills]
 * using [ChartAlphas] in one central place.
 */
@SuppressLint("RestrictedApi")
private fun buildCustomColorScheme(
    colorEngine: MaterialColorEngine,
    isDark: Boolean,
    themePrimary: Color,
): CustomColorScheme {
    // Vivid semantic color helper:
    // - Light mode preserves the light seed's true fidelity container tone.
    // - Dark mode uses M3's luminous dark-mode accent role so yellows/oranges glow cleanly
    //   on dark surfaces instead of turning olive/muddy.
    fun vividMetricColor(seed: Color): Color = if (isDark) {
        colorEngine.generateColorRoles(seed, isDark = true).accent
    } else {
        colorEngine.generateColorScheme(seed, isDark = false, variant = ColorSchemeVariant.FIDELITY).primaryContainer
    }

    // Deeper tier color for two-step scales (e.g., uvDarkGreen / uvDarkRed) so dark stops stay
    // visibly deeper than their bright companions in both light and dark modes.
    fun deepMetricColor(seed: Color): Color = colorEngine.generateColorScheme(
        seedColor = seed,
        isDark = isDark,
        variant = ColorSchemeVariant.FIDELITY,
    ).primaryContainer

    // Single Sun scheme & single Moon scheme (icons, borders, and chart lines all derive from M3 roles)
    val sunScheme = colorEngine.generateColorScheme(MaterialColors.Amber600, isDark, ColorSchemeVariant.FIDELITY)
    val moonScheme = colorEngine.generateColorScheme(MaterialColors.BlueA700, isDark, ColorSchemeVariant.FIDELITY)

    // Neutral chart chrome harmonized with the active Material You / theme primary
    val neutralScheme = colorEngine.generateColorScheme(
        seedColor = MaterialColors.Gray600,
        isDark = isDark,
        variant = ColorSchemeVariant.NEUTRAL,
        harmonizeWith = themePrimary,
    )
    val labelScheme = colorEngine.generateColorScheme(
        seedColor = MaterialColors.Gray700,
        isDark = isDark,
        variant = ColorSchemeVariant.NEUTRAL,
        contrast = ColorContrast.MEDIUM,
        harmonizeWith = themePrimary,
    )

    // Day sky, Moon background, and 4-step monotonic Twilight -> Night ramp
    val daySkyScheme = colorEngine.generateColorScheme(MaterialColors.LightBlue50, isDark, ColorSchemeVariant.TONAL_SPOT)
    val moonBgScheme = colorEngine.generateColorScheme(MaterialColors.Purple200, isDark, ColorSchemeVariant.TONAL_SPOT)
    val twilightScheme = colorEngine.generateColorScheme(MaterialColors.BlueGray500, isDark, ColorSchemeVariant.FIDELITY)

    // Air Mass horizon mist, wavy tile fills, Shadow neutral ramp & Irradiance dial scheme
    val amHorizonScheme = colorEngine.generateColorScheme(Color(0xFFCFD8DC), isDark, ColorSchemeVariant.FIDELITY)
    val amWaveScheme = colorEngine.generateColorScheme(Color(0xFF29B6F6), isDark, ColorSchemeVariant.TONAL_SPOT)
    val luxWaveScheme = colorEngine.generateColorScheme(Color(0xFFFFF59D), isDark, ColorSchemeVariant.TONAL_SPOT)
    val warmWaveScheme = colorEngine.generateColorScheme(Color(0xFFFF8C00), isDark, ColorSchemeVariant.TONAL_SPOT)
    val coolWaveScheme = colorEngine.generateColorScheme(Color(0xFF1976D2), isDark, ColorSchemeVariant.TONAL_SPOT)
    val shadowScheme = colorEngine.generateColorScheme(Color(0xFF424242), isDark, ColorSchemeVariant.NEUTRAL)
    val irrDialScheme = colorEngine.generateColorScheme(Color(0xFFE65100), isDark, ColorSchemeVariant.FIDELITY)

    // Dark-mode chart fills adapt the light-mode seed hues to bright, high-chroma HCT tones so
    // sky ramps and metric charts stay vibrant and legible over dark surfaces without altering light mode.
    fun darkAdaptedColor(
        seed: Color,
        darkTone: Double,
        darkChroma: Double? = null,
        lightColor: () -> Color = { vividMetricColor(seed) },
    ): Color = if (isDark) {
        val hct = Hct.fromInt(seed.toArgb())
        Color(Hct.from(hct.hue, darkChroma ?: hct.chroma, darkTone).toInt())
    } else {
        lightColor()
    }

    // Opaque base colors (1.0f)
    val dayBackground = darkAdaptedColor(Color(0xFF4FC3F7), darkTone = 68.0, darkChroma = 38.0) {
        daySkyScheme.primaryContainer
    }
    val civilTwilight = darkAdaptedColor(MaterialColors.BlueGray500, darkTone = 54.0, darkChroma = 28.0) {
        twilightScheme.outlineVariant
    }
    val nauticalTwilight = darkAdaptedColor(MaterialColors.BlueGray500, darkTone = 42.0, darkChroma = 24.0) {
        twilightScheme.outline
    }
    val astronomicalTwilight = darkAdaptedColor(MaterialColors.BlueGray500, darkTone = 30.0, darkChroma = 18.0) {
        twilightScheme.onSurfaceVariant
    }
    val nightBackground = darkAdaptedColor(MaterialColors.BlueGray500, darkTone = 18.0, darkChroma = 12.0) {
        twilightScheme.onSurface
    }
    val elapsedDay = vividMetricColor(MaterialColors.Yellow500)
    val elapsedMoon = vividMetricColor(MaterialColors.LightBlueA100)
    val elapsedNight = neutralScheme.outlineVariant
    val elapsedOther = neutralScheme.outlineVariant
    val moonBackground = darkAdaptedColor(MaterialColors.Purple200, darkTone = 66.0, darkChroma = 34.0) {
        moonBgScheme.primaryContainer
    }

    val amZenith = darkAdaptedColor(Color(0xFF29B6F6), darkTone = 78.0)
    val amHorizon = darkAdaptedColor(Color(0xFFCFD8DC), darkTone = 54.0, darkChroma = 12.0) {
        amHorizonScheme.surfaceContainerHighest
    }
    val amWave = amWaveScheme.primaryContainer
    val shadowShort = darkAdaptedColor(Color(0xFF9E9E9E), darkTone = 76.0, darkChroma = 4.0) {
        shadowScheme.surfaceContainerHighest
    }
    val shadowLong = darkAdaptedColor(Color(0xFF616161), darkTone = 42.0, darkChroma = 4.0) {
        shadowScheme.onSurfaceVariant
    }
    val shadowDial = if (isDark) shadowScheme.primary else shadowScheme.onSurfaceVariant
    val luxBright = darkAdaptedColor(Color(0xFFFFF59D), darkTone = 88.0)
    val luxDim = darkAdaptedColor(Color(0xFF5C6BC0), darkTone = 62.0)
    val luxWave = luxWaveScheme.primaryContainer
    val irrLow = darkAdaptedColor(Color(0xFFFFCC80), darkTone = 84.0)
    val irrMid = darkAdaptedColor(Color(0xFFFF9800), darkTone = 74.0)
    val irrHigh = darkAdaptedColor(Color(0xFFE65100), darkTone = 62.0)
    val irrDial = if (isDark) irrDialScheme.primary else irrDialScheme.primaryContainer
    val warmWave = warmWaveScheme.primaryContainer
    val coolWave = coolWaveScheme.primaryContainer

    val uvDarkGreen = darkAdaptedColor(Color(0xFF2E7D32), darkTone = 64.0) { deepMetricColor(Color(0xFF2E7D32)) }
    val uvGreen = darkAdaptedColor(Color(0xFF4CAF50), darkTone = 78.0)
    val uvYellow = darkAdaptedColor(Color(0xFFFFEB3B), darkTone = 88.0)
    val uvAmber = darkAdaptedColor(Color(0xFFFFC107), darkTone = 78.0)
    val uvOrange = darkAdaptedColor(Color(0xFFFF9800), darkTone = 73.0)
    val uvRed = darkAdaptedColor(Color(0xFFF44336), darkTone = 70.0)
    val uvDarkRed = darkAdaptedColor(Color(0xFFB71C1C), darkTone = 56.0) { deepMetricColor(Color(0xFFB71C1C)) }
    val uvPurple = darkAdaptedColor(Color(0xFF673AB7), darkTone = 70.0)

    val ct5500 = darkAdaptedColor(Color(0xFF81D4FA), darkTone = 82.0)
    val ct4000 = darkAdaptedColor(Color(0xFFFFF59D), darkTone = 88.0)
    val ct3000 = darkAdaptedColor(Color(0xFFFFB300), darkTone = 76.0)
    val ct2000 = darkAdaptedColor(Color(0xFFD84315), darkTone = 62.0)

    // Standardized chart fill alphas applied once here
    val skyAlpha = ChartAlphas.fillSky(isDark)
    val metricAlpha = ChartAlphas.fillMetric(isDark)
    val elapsedAlpha = ChartAlphas.fillElapsed(isDark)

    val fills = ChartFillColors(
        dayBackground = dayBackground.copy(alpha = skyAlpha),
        nightBackground = nightBackground.copy(alpha = skyAlpha),
        civilTwilight = civilTwilight.copy(alpha = skyAlpha),
        nauticalTwilight = nauticalTwilight.copy(alpha = skyAlpha),
        astronomicalTwilight = astronomicalTwilight.copy(alpha = skyAlpha),
        elapsedDay = elapsedDay.copy(alpha = elapsedAlpha),
        elapsedNight = elapsedNight.copy(alpha = elapsedAlpha),
        elapsedMoon = elapsedMoon.copy(alpha = elapsedAlpha),
        elapsedOther = elapsedOther.copy(alpha = elapsedAlpha),
        moonBackground = moonBackground.copy(alpha = skyAlpha),

        amZenith = amZenith.copy(alpha = metricAlpha),
        amHorizon = amHorizon.copy(alpha = metricAlpha),
        shadowShort = shadowShort.copy(alpha = metricAlpha),
        shadowLong = shadowLong.copy(alpha = metricAlpha),
        luxBright = luxBright.copy(alpha = metricAlpha),
        luxDim = luxDim.copy(alpha = metricAlpha),
        irrLow = irrLow.copy(alpha = metricAlpha),
        irrMid = irrMid.copy(alpha = metricAlpha),
        irrHigh = irrHigh.copy(alpha = metricAlpha),

        uvDarkGreen = uvDarkGreen.copy(alpha = metricAlpha),
        uvGreen = uvGreen.copy(alpha = metricAlpha),
        uvYellow = uvYellow.copy(alpha = metricAlpha),
        uvAmber = uvAmber.copy(alpha = metricAlpha),
        uvOrange = uvOrange.copy(alpha = metricAlpha),
        uvRed = uvRed.copy(alpha = metricAlpha),
        uvDarkRed = uvDarkRed.copy(alpha = metricAlpha),
        uvPurple = uvPurple.copy(alpha = metricAlpha),

        ct5500 = ct5500.copy(alpha = metricAlpha),
        ct4000 = ct4000.copy(alpha = metricAlpha),
        ct3000 = ct3000.copy(alpha = metricAlpha),
        ct2000 = ct2000.copy(alpha = metricAlpha),
    )

    return CustomColorScheme(
        dayBackground = dayBackground,
        civilTwilight = civilTwilight,
        nauticalTwilight = nauticalTwilight,
        astronomicalTwilight = astronomicalTwilight,
        nightBackground = nightBackground,
        elapsedDay = elapsedDay,
        elapsedMoon = elapsedMoon,
        elapsedNight = elapsedNight,
        elapsedOther = elapsedOther,
        moonBackground = moonBackground,

        sun = if (isDark) sunScheme.primaryContainer else sunScheme.primaryContainer,
        sunBorder = if (isDark) sunScheme.primary else sunScheme.primary,
        sunPath = sunScheme.primary,
        chartCurve = neutralScheme.onSurfaceVariant,
        chartLabel = labelScheme.onSurfaceVariant.copy(alpha = ChartAlphas.label(isDark)),
        dropLine = neutralScheme.outline,
        moon = if (isDark) moonScheme.primary else moonScheme.surfaceContainerHighest,
        moonUnlit = if (isDark) moonScheme.surfaceContainerHighest else moonScheme.primaryContainer,
        moonBorder = if (isDark) moonScheme.primaryFixedDim else moonScheme.primary,
        moonPath = moonScheme.primary,

        nightPrimary = if (isDark) Colors.DeepPurple900.primaryDark else Colors.DeepPurple900.primaryLight,
        nightOnPrimary = if (isDark) Colors.DeepPurple900.onPrimaryDark else Colors.DeepPurple900.onPrimaryLight,
        nightPrimaryContainer = if (isDark) Colors.DeepPurple900.primaryContainerDark else Colors.DeepPurple900.primaryContainerLight,
        nightOnPrimaryContainer = if (isDark) Colors.DeepPurple900.onPrimaryContainerDark else Colors.DeepPurple900.onPrimaryContainerLight,

        amZenith = amZenith,
        amHorizon = amHorizon,
        amWave = amWave,
        shadowShort = shadowShort,
        shadowLong = shadowLong,
        shadowDial = shadowDial,
        luxBright = luxBright,
        luxDim = luxDim,
        luxWave = luxWave,
        irrLow = irrLow,
        irrMid = irrMid,
        irrHigh = irrHigh,
        irrDial = irrDial,
        warmWave = warmWave,
        coolWave = coolWave,

        uvDarkGreen = uvDarkGreen,
        uvGreen = uvGreen,
        uvYellow = uvYellow,
        uvAmber = uvAmber,
        uvOrange = uvOrange,
        uvRed = uvRed,
        uvDarkRed = uvDarkRed,
        uvPurple = uvPurple,

        ct5500 = ct5500,
        ct4000 = ct4000,
        ct3000 = ct3000,
        ct2000 = ct2000,

        fills = fills,
    )
}

internal val LocalCustomColors = staticCompositionLocalOf<CustomColorScheme> {
    error("No custom colors provided")
}

@Composable
fun HeliosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    colorEngine: MaterialColorEngine = rememberMaterialColorEngine(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val customColors = remember(colorEngine, darkTheme, colorScheme.primary) {
        buildCustomColorScheme(
            colorEngine = colorEngine,
            isDark = darkTheme,
            themePrimary = colorScheme.primary,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = {
            CompositionLocalProvider(
                LocalCustomColors provides customColors
            ) {
                content()
            }
        }
    )
}