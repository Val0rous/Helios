package com.ephemeris.helios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ephemeris.helios.ui.composables.Navbar
import com.ephemeris.helios.ui.composables.TimeMachine
import com.ephemeris.helios.ui.composables.TopBar
import com.ephemeris.helios.ui.screens.Sun
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.Routes
import com.ephemeris.helios.utils.SolarEphemeris
import com.ephemeris.helios.utils.SunMetrics
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialStartDestination = intent.getStringExtra("startDestination") ?: Routes.Home.route
        setContent {
            navController = rememberNavController()
            var startDestination by remember { mutableStateOf(initialStartDestination) }
            var currentTime by remember { mutableStateOf(ZonedDateTime.now()) }
            var isAutoUpdateEnabled by remember { mutableStateOf(true) }
            var coordinates by remember { mutableStateOf(Coordinates(3.1, 11.99)) }

//            currentTime = ZonedDateTime.of(2026, 3, 27, 15, 0, 0, 0, ZoneId.of("UTC+1"))
            val events = SolarEphemeris.calculateDailyEvents(
                date = currentTime.toLocalDate(),
                latitude = coordinates.latitude,
                longitude = coordinates.longitude,
                tzOffsetHours = currentTime.offset.totalSeconds / 3600.0
            )
            val durations = SolarEphemeris.calculateDailyDurations(events)
            var currentSunPosition by remember{ mutableStateOf(SolarEphemeris.calculatePosition(currentTime, coordinates.latitude, coordinates.longitude)) }
            var dailyPeakMetrics by remember { mutableStateOf(SunMetrics.calculateMetrics(
                sunElevationDeg = events.solarNoonAltitude,
                observerAltitudeMeters = coordinates.altitude
            ))}
            var liveMetrics by remember { mutableStateOf(SunMetrics.calculateMetrics(
                sunElevationDeg = currentSunPosition.altitude,
                observerAltitudeMeters = coordinates.altitude
            ))}
            var oldDay = currentTime.toLocalDate()

            HeliosTheme {
                Scaffold(
                    //modifier
                    topBar = { TopBar({}) },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TimeMachine(
                                time = currentTime,
                                isAutoUpdate = isAutoUpdateEnabled,
                                onTimeChange = { currentTime = it },
                                onAutoUpdateChange = { isAutoUpdateEnabled = it },
                            )
                            Navbar(navController)
                        }
                    },
                ) { paddingValues ->
                    LaunchedEffect(Unit) {
                        savedInstanceState?.getString("NAVIGATION_STATE")?.let { savedRoute ->
                            startDestination = savedRoute
                        }
                    }
                    LaunchedEffect(isAutoUpdateEnabled) {
                        if (isAutoUpdateEnabled) {
                            while (true) {
                                delay(12000)
                                currentTime = ZonedDateTime.now()
                                currentSunPosition = SolarEphemeris.calculatePosition(currentTime, coordinates.latitude, coordinates.longitude)
                                if (oldDay != currentTime.toLocalDate()) {
                                    dailyPeakMetrics = SunMetrics.calculateMetrics(currentSunPosition.altitude, coordinates.altitude!!)
                                    oldDay = currentTime.toLocalDate()
                                }
                                liveMetrics = SunMetrics.calculateMetrics(currentSunPosition.altitude, coordinates.altitude!!)
                            }
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(paddingValues = paddingValues)
                    ) {
                        composable(Routes.Home.route) {
                            //Home()
                        }
                        composable(Routes.Exposure.route) {
                            //UV()
                        }
                        composable(Routes.Sun.route) {
                            Sun(
                                currentTime = currentTime,
                                coordinates = coordinates,
                                currentPosition = currentSunPosition,
                                events = events,
                                durations = durations,
                                dailyPeakMetrics = dailyPeakMetrics,
                                liveMetrics = liveMetrics
                            )
                        }
                        composable(Routes.Moon.route) {
                            //Moon()
                        }
                        composable(Routes.Planets.route) {
                            //Planets()
                        }
                    }
                }
//                HeliosApp()
            }
        }
    }
}

//@PreviewScreenSizes
//@Composable
//fun HeliosApp() {
//    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
//
//    NavigationSuiteScaffold(
//        navigationSuiteItems = {
//            AppDestinations.entries.forEach {
//                item(
//                    icon = {
//                        Icon(
//                            painterResource(it.icon),
//                            contentDescription = it.label
//                        )
//                    },
//                    label = { Text(it.label) },
//                    selected = it == currentDestination,
//                    onClick = { currentDestination = it }
//                )
//            }
//        }
//    ) {
//        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//            Greeting(
//                name = "Android",
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
//    }
//}

//enum class AppDestinations(
//    val label: String,
//    val icon: Int,
//) {
//    HOME("Home", R.drawable.ic_home),
//    SUN("Sun", R.drawable.ic_sunny),
//    MOON("Moon", R.drawable.ic_moon_stars),
//    PLANETS("Planets", R.drawable.ic_planet),
//}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    HeliosTheme {
//        Greeting("Android")
//    }
//}