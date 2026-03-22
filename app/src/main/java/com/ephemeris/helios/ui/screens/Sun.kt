package com.ephemeris.helios.ui.screens

import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import com.ephemeris.helios.ui.composables.PathCard
import kotlin.collections.copyOf
import kotlin.collections.toFloatArray
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

val hours = FloatArray(481) { round(it * 5f) / 100f }
val angles = FloatArray(hours.size) { Math.toDegrees(asin(cos(Math.toRadians(15.0)*(hours[it] - 12f)))).toFloat() }

fun toSin(list: FloatArray): FloatArray {
    return list.map { 90f * sin(Math.toRadians(it.toDouble())).toFloat() }.toFloatArray()
}

fun toCos(list: FloatArray): FloatArray {
    return list.map { 90f * cos(Math.toRadians(90.0 - it.toDouble())).toFloat() }.toFloatArray()
}

fun getAngles(lat: Float, dec: Float, toSin: Boolean = false, toCos: Boolean = false): FloatArray {
    val list = angles.map { dec + (90f - lat) * sin(Math.toRadians(it.toDouble())).toFloat() }.toFloatArray()
    if (toSin) return toSin(list)
    if (toCos) return toCos(list)
    return list
}

@Composable
fun Sun() {
    LazyColumn() {
        // lat 0, dec 0
        item { PathCard(hours, getAngles(0f, 0f)) }
        // lat 33.44, dec 0
        item { PathCard(hours, getAngles(33.44f, 0f)) }
        // lat 30, dec 0
        item { PathCard(hours, getAngles(30f, 0f)) }
        // lat 45, dec 0
        item { PathCard(hours, getAngles(45f, 0f)) }
        // lat 45, dec +23.44
        item { PathCard(hours, getAngles(45f, 23.44f)) }
        // lat 70, dec 10
        item { PathCard(hours, getAngles(70f, 10f)) }
        // lat 80, dec 0
        item { PathCard(hours, getAngles(80f, 0f)) }
        // lat 90, dec +23.44
        item { PathCard(hours, getAngles(90f, 23.44f)) }
        // lat 65, dec -23.44
        item { PathCard(hours, getAngles(65f, -23.44f)) }
    }
}