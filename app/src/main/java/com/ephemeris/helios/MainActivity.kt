package com.ephemeris.helios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ephemeris.helios.ui.composables.Navbar
import com.ephemeris.helios.ui.composables.TimeMachine
import com.ephemeris.helios.ui.composables.TopBar
import com.ephemeris.helios.ui.screens.Home
import com.ephemeris.helios.ui.screens.Sun
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.Routes
import com.ephemeris.helios.utils.calc.SeasonalEphemeris
import com.ephemeris.helios.utils.calc.SolarEphemeris
import com.ephemeris.helios.utils.calc.SunMetrics
import com.ephemeris.helios.utils.datastore.LocationDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

data class DayEphemerisData(
    val events: SolarEphemeris.DailyEvents,
    val durations: SolarEphemeris.DailyDurations,
    val dailyPeakMetrics: SunMetrics.SunMetricsResult,
    val seasonalEvents: SeasonalEphemeris.SeasonalEvents,
    val seasonalDailyEvents: SeasonalEphemeris.SeasonalDailyEvents
)

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialStartDestination = intent.getStringExtra("startDestination") ?: Routes.Home.route
        // Initialize DataStore repository
        val locationDataStore = LocationDataStore(this)

        setContent {
            navController = rememberNavController()
            var startDestination by remember { mutableStateOf(initialStartDestination) }
            var currentTime by remember { mutableStateOf(ZonedDateTime.now()) }
            var isAutoUpdateEnabled by remember { mutableStateOf(true) }
            val coordinates by locationDataStore.coordinatesFlow.collectAsState(
                initial = Coordinates(-33.8623, 151.2077)
            )

            var dayData by remember { mutableStateOf<DayEphemerisData?>(null)}
            var currentSunPosition by remember{ mutableStateOf<SolarEphemeris.SolarPosition?>(null) }
            var liveMetrics by remember { mutableStateOf<SunMetrics.SunMetricsResult?>(null)}

            // 1. Heavy Daily Math: Only recalculates when the DATE or LOCATION changes
            LaunchedEffect(coordinates, currentTime.toLocalDate()) {
                withContext(Dispatchers.Default) {
                    val date = currentTime.toLocalDate()
                    val tzOffsetHours = currentTime.offset.totalSeconds / 3600.0

                    val events = SolarEphemeris.calculateDailyEvents(date, coordinates.latitude, coordinates.longitude, tzOffsetHours)
                    val durations = SolarEphemeris.calculateDailyDurations(events)
                    val dailyPeakMetrics = SunMetrics.calculateMetrics(events.solarNoonAltitude, coordinates.altitude)

                    val seasonalEvents = SeasonalEphemeris.getSeasonalEvents(currentTime.year, ZoneId.systemDefault())
                    val mEq = SeasonalEphemeris.getDaily(seasonalEvents.marchEquinox, coordinates)
                    val jSo = SeasonalEphemeris.getDaily(seasonalEvents.juneSolstice, coordinates)
                    val sEq = SeasonalEphemeris.getDaily(seasonalEvents.septemberEquinox, coordinates)
                    val dSo = SeasonalEphemeris.getDaily(seasonalEvents.decemberSolstice, coordinates)

                    val seasonalDailyEvents = SeasonalEphemeris.SeasonalDailyEvents(
                        marchEquinoxDaylight = mEq.dayLength,
                        juneSolsticeDaylight = jSo.dayLength,
                        septemberEquinoxDaylight = sEq.dayLength,
                        decemberSolsticeDaylight = dSo.dayLength,
                        marchEquinoxSunAngle = mEq.solarNoonAltitude,
                        juneSolsticeSunAngle = jSo.solarNoonAltitude,
                        septemberEquinoxSunAngle = sEq.solarNoonAltitude,
                        decemberSolsticeSunAngle = dSo.solarNoonAltitude
                    )

                    // Assignment to Compose state is thread-safe
                    dayData = DayEphemerisData(events, durations, dailyPeakMetrics, seasonalEvents, seasonalDailyEvents)
                }
            }

            // 2. Lightweight Ticker: Runs every 12 seconds for live UI updates
            LaunchedEffect(isAutoUpdateEnabled, coordinates) {
                if (isAutoUpdateEnabled) {
                    while (true) {
                        withContext(Dispatchers.Default) {
//                            val newTime = ZonedDateTime.now()
//                            val pos = SolarEphemeris.calculatePosition(newTime, coordinates.latitude, coordinates.longitude)
                            val pos = SolarEphemeris.calculatePosition(currentTime, coordinates.latitude, coordinates.longitude)
                            val metrics = SunMetrics.calculateMetrics(pos.altitude, coordinates.altitude)

                            // Update states to trigger recomposition
//                            currentTime = newTime
                            currentSunPosition = pos
                            liveMetrics = metrics
                        }
                        delay(12000)
                    }
                }
            }

            // 3. Calculate time every second to update TimeMachine clock
            LaunchedEffect(isAutoUpdateEnabled, coordinates) {
                if (isAutoUpdateEnabled) {
                    while(true) {
                        withContext(Dispatchers.Default) {
                            val newTime = ZonedDateTime.now()
                            currentTime = newTime
                        }
                        delay(1000)
                    }
                }
            }

            HeliosTheme {
                Scaffold(
                    //modifier
                    topBar = { TopBar(
                        coordinates = coordinates,
                        onSaveCoordinates = { newCoordinates ->
                            lifecycleScope.launch {
                                locationDataStore.saveCoordinates(newCoordinates)
                            }
                        },
                        onLocationClick = {}) },
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) { paddingValues ->
                    LaunchedEffect(Unit) {
                        savedInstanceState?.getString("NAVIGATION_STATE")?.let { savedRoute ->
                            startDestination = savedRoute
                        }
                    }
                    // Guard clause: Don't render the heavy UI until the background threads finish their first pass
                    if (dayData == null || currentSunPosition == null || liveMetrics == null) {
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        return@Scaffold
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(paddingValues = paddingValues)
                    ) {
                        composable(Routes.Home.route) {
                            Home(
                                seasonalEvents = dayData!!.seasonalEvents,
                                seasonalDailyEvents = dayData!!.seasonalDailyEvents
                            )
                        }
                        composable(Routes.Exposure.route) {
                            //UV()
                        }
                        composable(Routes.Sun.route) {
                            Sun(
                                currentTime = currentTime,
                                coordinates = coordinates,
                                currentPosition = currentSunPosition!!,
                                events = dayData!!.events,
                                durations = dayData!!.durations,
                                dailyPeakMetrics = dayData!!.dailyPeakMetrics,
                                liveMetrics = liveMetrics!!
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