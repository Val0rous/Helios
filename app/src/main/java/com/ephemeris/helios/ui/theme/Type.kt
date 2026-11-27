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

/** Rounded Google Sans Flex (`ROND = 100`, `opsz = 36`, `wdth = 92`) used as the app-wide default font. */
val GoogleSansFlexRounded: FontFamily = FontFamily(
    AllFontWeights.flatMap { weight ->
        listOf(
            Font(
                resId = R.font.google_sans_flex,
                weight = weight,
                style = FontStyle.Normal,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight),
                    FontVariation.width(92f),
                    FontVariation.Setting("ROND", 100f),
                    FontVariation.Setting("opsz", 36f),
                ),
            ),
            Font(
                resId = R.font.google_sans_flex,
                weight = weight,
                style = FontStyle.Italic,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight),
                    FontVariation.width(92f),
                    FontVariation.Setting("ROND", 100f),
                    FontVariation.Setting("opsz", 36f),
                    FontVariation.Setting("slnt", -10f),
                ),
            ),
        )
    },
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(fontFamily = GoogleSansFlexRounded)
