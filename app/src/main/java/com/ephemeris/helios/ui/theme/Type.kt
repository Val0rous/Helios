package com.ephemeris.helios.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.ephemeris.helios.R

private val AllFontWeights = listOf(
    FontWeight.Thin,
    FontWeight.ExtraLight,
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
)

private fun googleSansFlexFamily(
    roundness: Float,
    opticalSize: Float,
    width: Float = 100f,
): FontFamily = FontFamily(
    AllFontWeights.flatMap { weight ->
        listOf(
            Font(
                resId = R.font.google_sans_flex,
                weight = weight,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight),
                    FontVariation.width(width),
                    FontVariation.Setting("ROND", roundness),
                    FontVariation.Setting("opsz", opticalSize),
                ),
            ),
            Font(
                resId = R.font.google_sans_flex,
                weight = weight,
                style = FontStyle.Italic,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight),
                    FontVariation.width(width),
                    FontVariation.Setting("ROND", roundness),
                    FontVariation.Setting("opsz", opticalSize),
                    FontVariation.Setting("slnt", -10f),
                ),
            ),
        )
    },
)

/** Standard Google Sans Flex variable font used across app copy, headers, and labels. */
val GoogleSansFlex: FontFamily = googleSansFlexFamily(
    roundness = 0f,
    opticalSize = 16f,
)

/** Rounded Google Sans Flex (`ROND = 100`) used for large display & metric numbers. */
val GoogleSansFlexRounded: FontFamily = googleSansFlexFamily(
    roundness = 100f,
    opticalSize = 36f,
    width = 92f,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val BaseTypography = Typography(fontFamily = GoogleSansFlex)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(
    fontFamily = GoogleSansFlex,
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GoogleSansFlexRounded),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GoogleSansFlexRounded),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GoogleSansFlexRounded),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GoogleSansFlexRounded),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GoogleSansFlexRounded),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GoogleSansFlexRounded),
    displayLargeEmphasized = BaseTypography.displayLargeEmphasized.copy(fontFamily = GoogleSansFlexRounded),
    displayMediumEmphasized = BaseTypography.displayMediumEmphasized.copy(fontFamily = GoogleSansFlexRounded),
    displaySmallEmphasized = BaseTypography.displaySmallEmphasized.copy(fontFamily = GoogleSansFlexRounded),
    headlineLargeEmphasized = BaseTypography.headlineLargeEmphasized.copy(fontFamily = GoogleSansFlexRounded),
    headlineMediumEmphasized = BaseTypography.headlineMediumEmphasized.copy(fontFamily = GoogleSansFlexRounded),
    headlineSmallEmphasized = BaseTypography.headlineSmallEmphasized.copy(fontFamily = GoogleSansFlexRounded),
)
