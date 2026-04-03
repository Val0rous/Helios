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
import com.ephemeris.helios.ui.screens.Moon
import com.ephemeris.helios.ui.screens.Sun
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.Coordinates
import com.ephemeris.helios.utils.Routes
import com.ephemeris.helios.utils.calc.DayEphemerisData
import com.ephemeris.helios.utils.calc.LiveUpdatesData
import com.ephemeris.helios.utils.calc.getDailyEphemerisData
import com.ephemeris.helios.utils.calc.getLiveUpdates
import com.ephemeris.helios.utils.datastore.LocationDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime


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
            var liveData by remember{ mutableStateOf<LiveUpdatesData?>(null) }

            // 1. Heavy Daily Math: Only recalculates when the DATE or LOCATION changes
            LaunchedEffect(coordinates, currentTime.toLocalDate()) {
                withContext(Dispatchers.Default) {
                    dayData = getDailyEphemerisData(currentTime, coordinates)
                }
            }

            // 2. Live Updates Ticker: Runs every 12 seconds for live UI updates, or when datetime is manually changed
            val manualTimeKey = if (!isAutoUpdateEnabled) currentTime else Unit
            LaunchedEffect(isAutoUpdateEnabled, coordinates, manualTimeKey) {
                if (isAutoUpdateEnabled) {
                    while (true) {
                        withContext(Dispatchers.Default) {
                            // Bypass the state race condition by grabbing the system time directly
                            liveData = getLiveUpdates(ZonedDateTime.now(), coordinates)
                        }
                        delay(12000)
                    }
                } else {
                    withContext(Dispatchers.Default) {
                        liveData = getLiveUpdates(currentTime, coordinates)
                    }
                }
            }

            // 3. Calculate time every second to update TimeMachine clock
            LaunchedEffect(isAutoUpdateEnabled, coordinates) {
                if (isAutoUpdateEnabled) {
                    do {
                        withContext(Dispatchers.Default) {
                            currentTime = ZonedDateTime.now()
                        }
                        delay(1000)
                    } while (isAutoUpdateEnabled)
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
                    if (dayData == null || liveData == null) {
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
                                currentSolarPosition = liveData!!.currentSunPosition,
                                events = dayData!!.events,
                                durations = dayData!!.durations,
                                dailyPeakMetrics = dayData!!.dailyPeakMetrics,
                                liveMetrics = liveData!!.liveSunMetrics
                            )
                        }
                        composable(Routes.Moon.route) {
                            Moon(
                                currentTime = currentTime,
                                coordinates = coordinates,
                                currentPosition = liveData!!.currentMoonPosition,
                                events = dayData!!.lunarEvents,
                                dailyPeakMetrics = dayData!!.lunarDailyPeakMetrics!!,
                                liveMetrics = liveData!!.liveMoonMetrics
                            )
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