package com.ephemeris.helios.utils

import com.ephemeris.helios.R

sealed class TimeMachineFilter(val label: Int) {
    object Hour : TimeMachineFilter(R.string.hour_abbreviation)
    object Day : TimeMachineFilter(R.string.day_abbreviation)
    object Year : TimeMachineFilter(R.string.year_abbreviation)

    companion object {
        val entries get() = listOf(Hour, Day, Year)
    }
}