package com.ephemeris.helios.utils

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Exposure: Routes("exposure")
    data object Sun: Routes("sun")
    data object Moon: Routes("moon")
    data object Planets: Routes("planets")
}