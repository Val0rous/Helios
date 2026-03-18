package com.ephemeris.helios.utils

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object UV: Routes("uv")
    data object Sun: Routes("sun")
    data object Moon: Routes("moon")
    data object Planets: Routes("planets")
}