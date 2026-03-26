package com.ephemeris.helios.utils

import androidx.compose.ui.res.stringResource
import com.ephemeris.helios.R

enum class LocationStatus(val icon: Int, val desc: Int) {
    CURRENT(R.drawable.ic_my_location, R.string.current_location),
    SEARCHING(R.drawable.ic_location_searching, R.string.searching_location),
    DISABLED(R.drawable.ic_location_disabled, R.string.location_disabled)
}