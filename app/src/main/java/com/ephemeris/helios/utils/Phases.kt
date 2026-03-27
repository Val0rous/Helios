package com.ephemeris.helios.utils

sealed class Phases(val desc: String) {
    object AboveHorizon : Phases("Over Horizon")
    object BelowHorizon : Phases("Under Horizon")
    object AlwaysAboveHorizon : Phases("Always Above")
    object AlwaysBelowHorizon : Phases("Always Below")

    sealed class Sun(desc: String): Phases(desc) {
        object Daylight : Sun("Daylight")
        object Night : Sun("Night")

        sealed class Twilight(sub: String = "", type: String = "Twilight") : Sun("$sub $type".trim()) {
            object Civil : Twilight("Civil")
            object Nautical : Twilight("Nautical")
            object Astronomical : Twilight("Astro")
        }

        sealed class Dawn(sub: String) : Twilight(sub, "Dawn") {
            object Civil : Dawn("Civil")
            object Nautical : Dawn("Nautical")
            object Astronomical : Dawn("Astro")
        }
        sealed class Dusk(sub: String) : Twilight(sub, "Dusk") {
            object Civil : Dusk("Civil")
            object Nautical : Dusk("Nautical")
            object Astronomical : Dusk("Astro")
        }
    }

    sealed class Moon(desc: String) : Phases(desc) {
        object New : Moon("New Moon")
        object WaxingCrescent : Moon("Waxing Crescent")
        object FirstQuarter : Moon("First Quarter")
        object WaxingGibbous : Moon("Waxing Gibbous")
        object Full : Moon("Full Moon")
        object WaningGibbous : Moon("Waning Gibbous")
        object LastQuarter : Moon("Last Quarter")
        object WaningCrescent : Moon("Waning Crescent")
    }
}

fun getSunPhase(altitude: Double): Phases {
    return when (altitude) {
        in -0.833..90.0 -> Phases.Sun.Daylight // Todo: don't hardcore number(s)
        in -6.0..-0.833 -> Phases.Sun.Twilight.Civil
        in -12.0..-6.0 -> Phases.Sun.Twilight.Nautical
        in -18.0..-12.0 -> Phases.Sun.Twilight.Astronomical
        else -> Phases.Sun.Night
    }
}