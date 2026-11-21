package com.ephemeris.helios.ui.theme

import android.annotation.SuppressLint
import androidx.annotation.IntRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.android.material.color.MaterialColors as AndroidMaterialColors
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeContent
import com.google.android.material.color.utilities.SchemeExpressive
import com.google.android.material.color.utilities.SchemeFidelity
import com.google.android.material.color.utilities.SchemeMonochrome
import com.google.android.material.color.utilities.SchemeNeutral
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.SchemeVibrant
import com.google.android.material.color.utilities.TonalPalette
import java.util.concurrent.ConcurrentHashMap
import org.koin.compose.koinInject

/**
 * Material 3 dynamic scheme generation style.
 *
 * Mirrors the official Material Color Utilities dynamic scheme variants.
 */
enum class ColorSchemeVariant {
    /** Standard Material 3 / Android dynamic theme balance (default). */
    TONAL_SPOT,

    /** Preserves the seed color closely across primary and container roles — ideal for brand or semantic colors. */
    FIDELITY,

    /** Keeps container roles faithful to the seed while harmonizing secondary and tertiary palettes. */
    CONTENT,

    /** High-chroma, punchier accents with vibrant secondary and tertiary palettes. */
    VIBRANT,

    /** Playful palette with noticeably rotated secondary and tertiary hues. */
    EXPRESSIVE,

    /** Subtle, near-grayscale palette tinted gently by the seed hue. */
    NEUTRAL,

    /** Pure grayscale palette. */
    MONOCHROME,
}

/**
 * Material 3 contrast levels mapped to the HCT engine's `contrastLevel` parameter.
 */
enum class ColorContrast(val level: Double) {
    STANDARD(0.0),
    MEDIUM(0.5),
    HIGH(1.0),
}

/**
 * Accessible Light/Dark Material 3 roles derived from a single custom seed color.
 *
 * Ideal for semantic accents, chart fills, chart lines, labels, and badges without needing
 * a full 48-token [ColorScheme]:
 * - [accent]: Main stroke, icon, or emphasized label color (Light tone 40 / Dark tone 80).
 * - [onAccent]: Text/icon color placed directly on top of [accent] (Light tone 100 / Dark tone 20).
 * - [accentContainer]: Soft container or chart area fill (Light tone 90 / Dark tone 30).
 * - [onAccentContainer]: Readable text/label color placed over [accentContainer] (Light tone 10 / Dark tone 90).
 */
@Immutable
data class SeedColorRoles(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

/**
 * Paired Light and Dark [ColorScheme]s generated from the same seed color.
 */
@Immutable
data class SeedColorSchemes(
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    fun forTheme(isDark: Boolean): ColorScheme = if (isDark) dark else light
}

/**
 * Paired Light and Dark [SeedColorRoles] generated from the same seed color.
 */
@Immutable
data class SeedColorRolePair(
    val light: SeedColorRoles,
    val dark: SeedColorRoles,
) {
    fun forTheme(isDark: Boolean): SeedColorRoles = if (isDark) dark else light
}

/**
 * DI-managed Material 3 color engine backed by Android's native Material Color Utilities (HCT).
 *
 * Generates full Light/Dark Compose [ColorScheme]s (`primary`, `onPrimary`, `secondary`, `surface`,
 * `onSurface`, `onSurfaceVariant`, surface containers, fixed roles, etc.) as well as lightweight
 * semantic [SeedColorRoles] and individual HCT tones from any single seed [Color].
 *
 * Registered as a Koin singleton in [com.ephemeris.helios.di.appModule] so all screens, cards,
 * and charts share a thread-safe, memoized cache.
 */
class MaterialColorEngine {

    private data class SchemeCacheKey(
        val seedArgb: Int,
        val isDark: Boolean,
        val variant: ColorSchemeVariant,
        val contrast: ColorContrast,
        val harmonizeWithArgb: Int?,
    )

    private data class RolesCacheKey(
        val seedArgb: Int,
        val isDark: Boolean,
        val harmonizeWithArgb: Int?,
    )

    private data class ToneCacheKey(
        val seedArgb: Int,
        val tone: Int,
        val harmonizeWithArgb: Int?,
    )

    private val schemeCache = ConcurrentHashMap<SchemeCacheKey, ColorScheme>()
    private val rolesCache = ConcurrentHashMap<RolesCacheKey, SeedColorRoles>()
    private val toneCache = ConcurrentHashMap<ToneCacheKey, Color>()

    /**
     * Generates a complete Material 3 [ColorScheme] (all 48 tokens including surface containers
     * and fixed roles) from a single [seedColor].
     *
     * @param seedColor Any base color (alpha is ignored for seed derivation).
     * @param isDark Whether to generate the dark or light scheme.
     * @param variant Material 3 scheme style (e.g., [ColorSchemeVariant.TONAL_SPOT] or [ColorSchemeVariant.FIDELITY]).
     * @param contrast Material 3 contrast level ([ColorContrast.STANDARD], [ColorContrast.MEDIUM], [ColorContrast.HIGH]).
     * @param harmonizeWith Optional reference color (such as `MaterialTheme.colorScheme.primary`) to
     *   subtly shift the seed's hue toward the active theme before generating the palette.
     */
    fun generateColorScheme(
        seedColor: Color,
        isDark: Boolean,
        variant: ColorSchemeVariant = ColorSchemeVariant.TONAL_SPOT,
        contrast: ColorContrast = ColorContrast.STANDARD,
        harmonizeWith: Color? = null,
    ): ColorScheme {
        val seedArgb = seedColor.toOpaqueArgb()
        val harmonizeArgb = harmonizeWith?.toOpaqueArgb()
        val key = SchemeCacheKey(seedArgb, isDark, variant, contrast, harmonizeArgb)
        return schemeCache.computeIfAbsent(key) {
            val effectiveArgb = harmonizeArgb?.let { harmonizeArgb(seedArgb, it) } ?: seedArgb
            buildColorScheme(effectiveArgb, isDark, variant, contrast)
        }
    }

    /**
     * Generates both Light and Dark Material 3 [ColorScheme]s from a single [seedColor].
     */
    fun generateLightAndDarkSchemes(
        seedColor: Color,
        variant: ColorSchemeVariant = ColorSchemeVariant.TONAL_SPOT,
        contrast: ColorContrast = ColorContrast.STANDARD,
        harmonizeWith: Color? = null,
    ): SeedColorSchemes = SeedColorSchemes(
        light = generateColorScheme(seedColor, isDark = false, variant = variant, contrast = contrast, harmonizeWith = harmonizeWith),
        dark = generateColorScheme(seedColor, isDark = true, variant = variant, contrast = contrast, harmonizeWith = harmonizeWith),
    )

    /**
     * Generates Material 3 custom semantic roles ([SeedColorRoles]) from a single [seedColor]
     * using Android's public [AndroidMaterialColors.getColorRoles] API.
     *
     * Ideal for chart lines, fills, legends, badges, and astronomical metric indicators.
     */
    fun generateColorRoles(
        seedColor: Color,
        isDark: Boolean,
        harmonizeWith: Color? = null,
    ): SeedColorRoles {
        val seedArgb = seedColor.toOpaqueArgb()
        val harmonizeArgb = harmonizeWith?.toOpaqueArgb()
        val key = RolesCacheKey(seedArgb, isDark, harmonizeArgb)
        return rolesCache.computeIfAbsent(key) {
            val effectiveArgb = harmonizeArgb?.let { harmonizeArgb(seedArgb, it) } ?: seedArgb
            val roles = AndroidMaterialColors.getColorRoles(effectiveArgb, !isDark)
            SeedColorRoles(
                accent = Color(roles.accent),
                onAccent = Color(roles.onAccent),
                accentContainer = Color(roles.accentContainer),
                onAccentContainer = Color(roles.onAccentContainer),
            )
        }
    }

    /**
     * Generates both Light and Dark [SeedColorRoles] from a single [seedColor].
     */
    fun generateLightAndDarkRoles(
        seedColor: Color,
        harmonizeWith: Color? = null,
    ): SeedColorRolePair = SeedColorRolePair(
        light = generateColorRoles(seedColor, isDark = false, harmonizeWith = harmonizeWith),
        dark = generateColorRoles(seedColor, isDark = true, harmonizeWith = harmonizeWith),
    )

    /**
     * Picks a specific Material 3 HCT [tone] (`0` = black .. `100` = white) from the tonal palette
     * of [seedColor], preserving its perceptual hue and chroma.
     *
     * Useful when building custom chart gradients or subtle fills that still match M3 tone ramps.
     */
    @SuppressLint("RestrictedApi")
    fun generateTonalColor(
        seedColor: Color,
        @IntRange(from = 0, to = 100) tone: Int,
        harmonizeWith: Color? = null,
    ): Color {
        val clampedTone = tone.coerceIn(0, 100)
        val seedArgb = seedColor.toOpaqueArgb()
        val harmonizeArgb = harmonizeWith?.toOpaqueArgb()
        val key = ToneCacheKey(seedArgb, clampedTone, harmonizeArgb)
        return toneCache.computeIfAbsent(key) {
            val effectiveArgb = harmonizeArgb?.let { harmonizeArgb(seedArgb, it) } ?: seedArgb
            Color(TonalPalette.fromInt(effectiveArgb).tone(clampedTone))
        }
    }

    /**
     * Harmonizes [color] toward [toward] in HCT color space (shifting hue slightly without
     * losing semantic identity — e.g., keeping a yellow sun tint looking cohesive against a
     * blue wallpaper theme). Preserves the original alpha of [color].
     */
    fun harmonize(color: Color, toward: Color): Color {
        val harmonized = Color(harmonizeArgb(color.toOpaqueArgb(), toward.toOpaqueArgb()))
        return if (color.alpha < 1f) harmonized.copy(alpha = color.alpha) else harmonized
    }

    /**
     * Clears all memoized schemes, roles, and tonal entries.
     */
    fun clearCache() {
        schemeCache.clear()
        rolesCache.clear()
        toneCache.clear()
    }

    @SuppressLint("RestrictedApi")
    private fun harmonizeArgb(colorArgb: Int, towardArgb: Int): Int {
        return Blend.harmonize(colorArgb, towardArgb)
    }

    @SuppressLint("RestrictedApi")
    private fun buildColorScheme(
        seedArgb: Int,
        isDark: Boolean,
        variant: ColorSchemeVariant,
        contrast: ColorContrast,
    ): ColorScheme {
        val sourceHct = Hct.fromInt(seedArgb)
        val scheme: DynamicScheme = when (variant) {
            ColorSchemeVariant.TONAL_SPOT -> SchemeTonalSpot(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.FIDELITY -> SchemeFidelity(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.CONTENT -> SchemeContent(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.VIBRANT -> SchemeVibrant(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.EXPRESSIVE -> SchemeExpressive(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.NEUTRAL -> SchemeNeutral(sourceHct, isDark, contrast.level)
            ColorSchemeVariant.MONOCHROME -> SchemeMonochrome(sourceHct, isDark, contrast.level)
        }
        // Keep MaterialDynamicColors local per build so DynamicColor's internal hctCache stays thread-confined.
        val mdc = MaterialDynamicColors()

        return ColorScheme(
            primary = Color(mdc.primary().getArgb(scheme)),
            onPrimary = Color(mdc.onPrimary().getArgb(scheme)),
            primaryContainer = Color(mdc.primaryContainer().getArgb(scheme)),
            onPrimaryContainer = Color(mdc.onPrimaryContainer().getArgb(scheme)),
            inversePrimary = Color(mdc.inversePrimary().getArgb(scheme)),
            secondary = Color(mdc.secondary().getArgb(scheme)),
            onSecondary = Color(mdc.onSecondary().getArgb(scheme)),
            secondaryContainer = Color(mdc.secondaryContainer().getArgb(scheme)),
            onSecondaryContainer = Color(mdc.onSecondaryContainer().getArgb(scheme)),
            tertiary = Color(mdc.tertiary().getArgb(scheme)),
            onTertiary = Color(mdc.onTertiary().getArgb(scheme)),
            tertiaryContainer = Color(mdc.tertiaryContainer().getArgb(scheme)),
            onTertiaryContainer = Color(mdc.onTertiaryContainer().getArgb(scheme)),
            background = Color(mdc.background().getArgb(scheme)),
            onBackground = Color(mdc.onBackground().getArgb(scheme)),
            surface = Color(mdc.surface().getArgb(scheme)),
            onSurface = Color(mdc.onSurface().getArgb(scheme)),
            surfaceVariant = Color(mdc.surfaceVariant().getArgb(scheme)),
            onSurfaceVariant = Color(mdc.onSurfaceVariant().getArgb(scheme)),
            surfaceTint = Color(mdc.surfaceTint().getArgb(scheme)),
            inverseSurface = Color(mdc.inverseSurface().getArgb(scheme)),
            inverseOnSurface = Color(mdc.inverseOnSurface().getArgb(scheme)),
            error = Color(mdc.error().getArgb(scheme)),
            onError = Color(mdc.onError().getArgb(scheme)),
            errorContainer = Color(mdc.errorContainer().getArgb(scheme)),
            onErrorContainer = Color(mdc.onErrorContainer().getArgb(scheme)),
            outline = Color(mdc.outline().getArgb(scheme)),
            outlineVariant = Color(mdc.outlineVariant().getArgb(scheme)),
            scrim = Color(mdc.scrim().getArgb(scheme)),
            surfaceBright = Color(mdc.surfaceBright().getArgb(scheme)),
            surfaceDim = Color(mdc.surfaceDim().getArgb(scheme)),
            surfaceContainer = Color(mdc.surfaceContainer().getArgb(scheme)),
            surfaceContainerHigh = Color(mdc.surfaceContainerHigh().getArgb(scheme)),
            surfaceContainerHighest = Color(mdc.surfaceContainerHighest().getArgb(scheme)),
            surfaceContainerLow = Color(mdc.surfaceContainerLow().getArgb(scheme)),
            surfaceContainerLowest = Color(mdc.surfaceContainerLowest().getArgb(scheme)),
            primaryFixed = Color(mdc.primaryFixed().getArgb(scheme)),
            primaryFixedDim = Color(mdc.primaryFixedDim().getArgb(scheme)),
            onPrimaryFixed = Color(mdc.onPrimaryFixed().getArgb(scheme)),
            onPrimaryFixedVariant = Color(mdc.onPrimaryFixedVariant().getArgb(scheme)),
            secondaryFixed = Color(mdc.secondaryFixed().getArgb(scheme)),
            secondaryFixedDim = Color(mdc.secondaryFixedDim().getArgb(scheme)),
            onSecondaryFixed = Color(mdc.onSecondaryFixed().getArgb(scheme)),
            onSecondaryFixedVariant = Color(mdc.onSecondaryFixedVariant().getArgb(scheme)),
            tertiaryFixed = Color(mdc.tertiaryFixed().getArgb(scheme)),
            tertiaryFixedDim = Color(mdc.tertiaryFixedDim().getArgb(scheme)),
            onTertiaryFixed = Color(mdc.onTertiaryFixed().getArgb(scheme)),
            onTertiaryFixedVariant = Color(mdc.onTertiaryFixedVariant().getArgb(scheme)),
        )
    }

    private fun Color.toOpaqueArgb(): Int = toArgb() or OPAQUE_ALPHA_MASK

    private companion object {
        const val OPAQUE_ALPHA_MASK: Int = 0xFF000000.toInt()
    }
}

/**
 * Resolves the Koin singleton [MaterialColorEngine] at runtime, falling back to a local
 * remembered instance in Compose `@Preview` mode where Koin is not initialized.
 */
@Composable
fun rememberMaterialColorEngine(): MaterialColorEngine {
    return if (LocalInspectionMode.current) {
        remember { MaterialColorEngine() }
    } else {
        koinInject()
    }
}

/**
 * Remembers a full Material 3 [ColorScheme] derived from [seedColor] via [MaterialColorEngine].
 */
@Composable
fun rememberSeedColorScheme(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    variant: ColorSchemeVariant = ColorSchemeVariant.TONAL_SPOT,
    contrast: ColorContrast = ColorContrast.STANDARD,
    harmonizeWith: Color? = null,
    colorEngine: MaterialColorEngine = rememberMaterialColorEngine(),
): ColorScheme = remember(seedColor, isDark, variant, contrast, harmonizeWith, colorEngine) {
    colorEngine.generateColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        variant = variant,
        contrast = contrast,
        harmonizeWith = harmonizeWith,
    )
}

/**
 * Remembers accessible Light/Dark [SeedColorRoles] derived from [seedColor] via [MaterialColorEngine].
 */
@Composable
fun rememberSeedColorRoles(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    harmonizeWith: Color? = null,
    colorEngine: MaterialColorEngine = rememberMaterialColorEngine(),
): SeedColorRoles = remember(seedColor, isDark, harmonizeWith, colorEngine) {
    colorEngine.generateColorRoles(
        seedColor = seedColor,
        isDark = isDark,
        harmonizeWith = harmonizeWith,
    )
}

/**
 * Applies a seed-derived Material 3 [ColorScheme] to a specific subtree (for example, a Sun card,
 * Moon card, or metric detail section) while preserving the app's typography and shapes.
 */
@Composable
fun SeededMaterialTheme(
    seedColor: Color,
    isDark: Boolean = isSystemInDarkTheme(),
    variant: ColorSchemeVariant = ColorSchemeVariant.TONAL_SPOT,
    contrast: ColorContrast = ColorContrast.STANDARD,
    harmonizeWith: Color? = null,
    colorEngine: MaterialColorEngine = rememberMaterialColorEngine(),
    content: @Composable () -> Unit,
) {
    val seededScheme = rememberSeedColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        variant = variant,
        contrast = contrast,
        harmonizeWith = harmonizeWith,
        colorEngine = colorEngine,
    )
    MaterialTheme(
        colorScheme = seededScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
