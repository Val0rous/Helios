package com.ephemeris.helios

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.ephemeris.helios.utils.HeliosViewModel
import com.ephemeris.helios.utils.LocationPermissionWrapper
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.Routes
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var locationService: LocationService
    private val viewModel: HeliosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locationService = LocationService(this)
        val initialStartDestination = intent.getStringExtra("startDestination") ?: Routes.Home.route
        // Initialize DataStore repository

        setContent {
            navController = rememberNavController()
            val context = LocalContext.current
            var startDestination by remember { mutableStateOf(initialStartDestination) }
            val coordinates by viewModel.coordinatesState.collectAsState()
            var isContinuousGPSTrackingEnabled by remember { mutableStateOf(false) }

            val snackbarHostState = remember { SnackbarHostState() }


            // 1. Heavy Daily Math: Only recalculates when the DATE or LOCATION changes
            LaunchedEffect(coordinates, viewModel.currentTime.toLocalDate()) {
                coordinates?.let { viewModel.updateDayData(it) }
            }

            // 2. Live Updates Ticker: Runs every 12 seconds for live UI updates, or when datetime is manually changed
            val manualTimeKey = if (!viewModel.isAutoUpdateEnabled) viewModel.currentTime else Unit
            LaunchedEffect(viewModel.isAutoUpdateEnabled, coordinates, manualTimeKey) {
                coordinates?.let {
                    while (true) {
                        viewModel.startLiveUpdatesTicker(it)
                        if (!viewModel.isAutoUpdateEnabled) break   // Stop looping if manual
                        delay(12000)
                    }
                }
            }

            HeliosTheme {
                // Wrapper handles all dialogs and lifecycle automatically
                LocationPermissionWrapper(
                    locationService = locationService,
                    snackbarHostState = snackbarHostState,
                    isContinuousGPSTrackingEnabled = isContinuousGPSTrackingEnabled
                ) { requestOneOffLocation, startContinuousTracking, stopTracking ->

                    Scaffold(
                        //modifier
                        topBar = {
                            coordinates?.let { currentCoords ->
                                TopBar(
                                    coordinates = currentCoords,
                                    onSaveCoordinates = { viewModel.saveCoordinates(it) },
                                    onLocationClick = { requestOneOffLocation() },
                                    locationService = locationService
                                )
                            }
                        },
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TimeMachine(
                                    time = viewModel.currentTime,
                                    isAutoUpdate = viewModel.isAutoUpdateEnabled,
                                    onTimeChange = { viewModel.currentTime = it },
                                    onAutoUpdateChange = { viewModel.isAutoUpdateEnabled = it },
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
                        if (coordinates == null || viewModel.dayData == null || viewModel.liveData == null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(if (coordinates == null) "Waiting for location..." else "Calculating Ephemeris...")
                                }
                            }

                            // If coordinates are null, proactively ask for location on first launch
                            LaunchedEffect(Unit) {
                                if (coordinates == null) requestOneOffLocation()
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
                                    seasonalEvents = viewModel.dayData!!.seasonalEvents,
                                    seasonalDailyEvents = viewModel.dayData!!.seasonalDailyEvents
                                )
                            }
                            composable(Routes.Exposure.route) {
                                //UV()
                            }
                            composable(Routes.Sun.route) {
                                Sun(
                                    currentTime = viewModel.currentTime,
                                    coordinates = coordinates!!,
                                    currentSolarPosition = viewModel.liveData!!.currentSunPosition,
                                    events = viewModel.dayData!!.events,
                                    durations = viewModel.dayData!!.durations,
                                    dailyPeakMetrics = viewModel.dayData!!.dailyPeakMetrics,
                                    liveMetrics = viewModel.liveData!!.liveSunMetrics
                                )
                            }
                            composable(Routes.Moon.route) {
                                Moon(
                                    currentTime = viewModel.currentTime,
                                    coordinates = coordinates!!,
                                    currentPosition = viewModel.liveData!!.currentMoonPosition,
                                    events = viewModel.dayData!!.lunarEvents,
                                    dailyPeakMetrics = viewModel.dayData!!.lunarDailyPeakMetrics!!,
                                    liveMetrics = viewModel.liveData!!.liveMoonMetrics
                                )
                            }
                            composable(Routes.Planets.route) {
                                //Planets()
                            }
                        }
                    }
                }
//                HeliosApp()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        locationService.pauseLocationRequest()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        locationService.resumeLocationRequest()
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