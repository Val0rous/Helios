package com.ephemeris.helios.utils

import com.ephemeris.helios.R

enum class LocationStatus(val icon: Int, val desc: String) {
    CURRENT(R.drawable.ic_my_location, "Current Location"),
    SEARCHING(R.drawable.ic_location_searching, "Searching Location..."),
    DISABLED(R.drawable.ic_location_disabled, "Location disabled")
}