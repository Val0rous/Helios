package com.ephemeris.helios.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

data class CustomColorScheme(
    val dayBackground: Color,
    val nightBackground: Color,
    val civilTwilight: Color,
    val nauticalTwilight: Color,
    val astronomicalTwilight: Color,
    val elapsedDay: Color,
    val elapsedNight: Color,
//    val dayBackground: Color,
//    val nightBackground: Color,
    val sun: Color,
    val sunPath: Color,
    val dropLine: Color,
    val moon: Color,
    val moonPath: Color,
    val nightPrimary: Color,
    val nightOnPrimary: Color,
    val nightPrimaryContainer: Color,
    val nightOnPrimaryContainer: Color,

    // --- NEW: Gradient Theme Colors ---
    // Air Mass (Clear Sky to Hazy Horizon)
    val amZenith: Color = Color(0xFF29B6F6).copy(alpha = 0.5f), // Clear Light Blue
    val amHorizon: Color = Color(0xFFCFD8DC).copy(alpha = 0.5f), // Hazy Grey/White
    // Shadows (Light/Short to Dark/Long)
    val shadowShort: Color = Color(0xFFE0E0E0).copy(alpha = 0.5f),
    val shadowLong: Color = Color(0xFF424242).copy(alpha = 0.5f),
    // Illuminance (Blinding Light to Dim)
    val luxBright: Color = Color(0xFFFFF59D).copy(alpha = 0.6f), // Glowing Pale Yellow
    val luxDim: Color = Color(0xFF5C6BC0).copy(alpha = 0.3f),   // Dim Twilight Blue
    // Irradiance Heat Map (Warm to Hot)
    val irrLow: Color = Color(0xFFFFCC80).copy(alpha = 0.4f),   // Soft Dawn Gold
    val irrMid: Color = Color(0xFFFF9800).copy(alpha = 0.5f),   // Orange Energy
    val irrHigh: Color = Color(0xFFE65100).copy(alpha = 0.6f),  // Intense Heat Red
)

private val DarkCustomColors = CustomColorScheme(
    dayBackground = MaterialColors.LightBlue50.copy(alpha = 0.7f),
    nightBackground = MaterialColors.Gray900.copy(alpha = 0.7f),
    civilTwilight = MaterialColors.BlueGray200.copy(alpha = 0.7f),
    nauticalTwilight = MaterialColors.BlueGray500.copy(alpha = 0.7f),
    astronomicalTwilight = MaterialColors.Gray700.copy(alpha = 0.7f),
    elapsedDay = MaterialColors.Yellow500.copy(alpha = 0.5f),
    elapsedNight = MaterialColors.Gray600.copy(alpha = 0.25f),
//    dayBackground = MaterialColors.Yellow50.copy(alpha = 0.15f),
//    nightBackground = MaterialColors.Indigo50.copy(alpha = 0.15f),
    sun = MaterialColors.Yellow700,
    sunPath = MaterialColors.Yellow800,
    dropLine = MaterialColors.Gray300,
    moon = MaterialColors.BlueA100,
    moonPath = MaterialColors.BlueA700,

    nightPrimary = Colors.DeepPurple900.primaryDark,
    nightOnPrimary = Colors.DeepPurple900.onPrimaryDark,
    nightPrimaryContainer = Colors.DeepPurple900.primaryContainerDark,
    nightOnPrimaryContainer = Colors.DeepPurple900.onPrimaryContainerDark
)

private val LightCustomColors = CustomColorScheme(
    dayBackground = MaterialColors.LightBlue50.copy(alpha = 0.6f),
    nightBackground = MaterialColors.Gray900.copy(alpha = 0.6f),
    civilTwilight = MaterialColors.BlueGray200.copy(alpha = 0.6f),
    nauticalTwilight = MaterialColors.BlueGray500.copy(alpha = 0.6f),
    astronomicalTwilight = MaterialColors.Gray700.copy(alpha = 0.6f),
    elapsedDay = MaterialColors.Yellow500.copy(alpha = 0.35f),
    elapsedNight = MaterialColors.Gray400.copy(alpha = 0.3f),
//    dayBackground = MaterialColors.Yellow50.copy(alpha = 0.2f),
//    nightBackground = MaterialColors.Indigo50.copy(alpha = 0.2f),
    sun = MaterialColors.Amber600,
    sunPath = MaterialColors.Orange800,
    dropLine = MaterialColors.Gray600,
    moon = MaterialColors.BlueA700,
    moonPath = MaterialColors.LightBlue500,

    nightPrimary = Colors.DeepPurple900.primaryLight,
    nightOnPrimary = Colors.DeepPurple900.onPrimaryLight,
    nightPrimaryContainer = Colors.DeepPurple900.primaryContainerLight,
    nightOnPrimaryContainer = Colors.DeepPurple900.onPrimaryContainerLight
)

internal val LocalCustomColors = staticCompositionLocalOf<CustomColorScheme> {
    error("No custom colors provided")
}

@Composable
fun HeliosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalCustomColors provides customColors
            ) {
                content()
            }
        }
    )
}