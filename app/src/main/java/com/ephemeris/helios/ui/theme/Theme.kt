package com.ephemeris.helios.ui.theme

import android.app.Activity
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
    val day: Color,
    val night: Color,
    val civilTwilight: Color,
    val nauticalTwilight: Color,
    val astronomicalTwilight: Color,
    val elapsedDay: Color,
    val elapsedNight: Color,
    val dayBackground: Color,
    val nightBackground: Color,
    val sun: Color,
    val moon: Color,
)

private val DarkCustomColors = CustomColorScheme(
    day = MaterialColors.LightBlue50.copy(alpha = 0.7f),
    night = MaterialColors.Gray900.copy(alpha = 0.7f),
    civilTwilight = MaterialColors.BlueGray200.copy(alpha = 0.7f),
    nauticalTwilight = MaterialColors.BlueGray500.copy(alpha = 0.7f),
    astronomicalTwilight = MaterialColors.Gray700.copy(alpha = 0.7f),
    elapsedDay = MaterialColors.Yellow500.copy(alpha = 0.5f),
    elapsedNight = MaterialColors.Gray600.copy(alpha = 0.25f),
    dayBackground = MaterialColors.Yellow50.copy(alpha = 0.15f),
    nightBackground = MaterialColors.Indigo50.copy(alpha = 0.15f),
    sun = MaterialColors.Orange400,
    moon = MaterialColors.Gray500
)

private val LightCustomColors = CustomColorScheme(
    day = MaterialColors.LightBlue50.copy(alpha = 0.6f),
    night = MaterialColors.Gray900.copy(alpha = 0.6f),
    civilTwilight = MaterialColors.BlueGray200.copy(alpha = 0.6f),
    nauticalTwilight = MaterialColors.BlueGray500.copy(alpha = 0.6f),
    astronomicalTwilight = MaterialColors.Gray700.copy(alpha = 0.6f),
    elapsedDay = MaterialColors.Yellow500.copy(alpha = 0.35f),
    elapsedNight = MaterialColors.Gray400.copy(alpha = 0.3f),
    dayBackground = MaterialColors.Yellow50.copy(alpha = 0.2f),
    nightBackground = MaterialColors.Indigo50.copy(alpha = 0.2f),
    sun = MaterialColors.Amber600,
    moon = MaterialColors.Gray700
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