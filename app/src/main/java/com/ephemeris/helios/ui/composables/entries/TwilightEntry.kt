package com.ephemeris.helios.ui.composables.entries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R
import com.ephemeris.helios.ui.theme.MaterialColors

@Composable
fun TwilightEntry(
    title: Int,
    subtitle: Int,
    civilTime: String = "",
    civilDuration: String = "",
    nauticalTime: String = "",
    nauticalDuration: String = "",
    astroTime: String = "",
    astroDuration: String = "",
    nightTime: String = "",
    nightDuration: String = ""
) {
    val civilColor = MaterialColors.DeepPurple200
    val nauticalColor = MaterialColors.DeepPurple300
    val astroColor = MaterialColors.DeepPurple500
    val twilightColor = MaterialColors.DeepPurple500
    val nightColor = MaterialColors.DeepPurple900
    HeaderEntry(text = stringResource(title), textVariant = "(${stringResource(subtitle)})", color = twilightColor)
    TextEntryHours(label = stringResource(R.string.civil), time = civilTime, duration = civilDuration, color = civilColor)
    TextEntryHours(label = stringResource(R.string.nautical), time = nauticalTime, duration = nauticalDuration, color = nauticalColor)
    TextEntryHours(label = stringResource(R.string.astro), time = astroTime, duration = astroDuration, color = astroColor)
//    TextEntryHours(label = stringResource(R.string.night), time = nightTime, duration = nightDuration, color = nightColor)
}