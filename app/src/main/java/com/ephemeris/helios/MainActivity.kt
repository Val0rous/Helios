package com.ephemeris.helios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
    private val vm: HeliosViewModel by viewModels()

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
            val coordinates by vm.coordinatesState.collectAsState()
            var isContinuousGPSTrackingEnabled by remember { mutableStateOf(false) }

            val snackbarHostState = remember { SnackbarHostState() }


            // 1. Heavy Daily Math: Only recalculates when the DATE or LOCATION changes
            LaunchedEffect(coordinates, vm.currentTime.toLocalDate()) {
                coordinates?.let { vm.updateDayData(it) }
            }

            // 2. Live Updates Ticker: Runs every 12 seconds for live UI updates, or when datetime is manually changed
            val manualTimeKey = if (!vm.isAutoUpdateEnabled) vm.currentTime else Unit
            LaunchedEffect(vm.isAutoUpdateEnabled, coordinates, manualTimeKey) {
                coordinates?.let {
                    while (true) {
                        vm.startLiveUpdatesTicker(it)
                        if (!vm.isAutoUpdateEnabled) break   // Stop looping if manual
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
                                    currentTime = vm.currentTime,
                                    coordinates = currentCoords,
                                    onSaveCoordinates = { vm.saveCoordinates(it) },
                                    onLocationClick = { requestOneOffLocation() },
                                    isTracking = isContinuousGPSTrackingEnabled,
                                    onToggleTracking = { enableTracking ->
                                        isContinuousGPSTrackingEnabled = enableTracking
                                        if (enableTracking) {
                                            startContinuousTracking(10000L)
                                        } else {
                                            stopTracking()
                                        }
                                    },
                                    locationService = locationService
                                )
                            }
                        },
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TimeMachine(
                                    time = vm.currentTime,
                                    isAutoUpdate = vm.isAutoUpdateEnabled,
                                    onTimeChange = { vm.currentTime = it },
                                    onAutoUpdateChange = { vm.isAutoUpdateEnabled = it },
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
                        // 1. Guard clause: Wait for the physical disk to finish reading
                        if (!vm.isDataStoreLoaded) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Reading saved data...")
                                }
                            }
                            return@Scaffold
                        }

                        // 2. Guard clause: Disk loaded, but it's empty (First Launch). Fire the GPS.
                        if (coordinates == null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Waiting for GPS location...")
                                }
                            }

                            LaunchedEffect(Unit) {
                                requestOneOffLocation()
                            }
                            return@Scaffold
                        }

                        // 3. Guard clause: Coordinates exist, but the background math is still calculating
                        if (vm.dayData == null || vm.liveData == null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Calculating Ephemeris...")
                                }
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
                                    seasonalEvents = vm.dayData!!.seasonalEvents,
                                    seasonalDailyEvents = vm.dayData!!.seasonalDailyEvents
                                )
                            }
                            composable(Routes.Exposure.route) {
                                //UV()
                            }
                            composable(Routes.Sun.route) {
                                Sun(
                                    currentTime = vm.currentTime,
                                    coordinates = coordinates!!,
                                    currentSolarPosition = vm.liveData!!.currentSunPosition,
                                    events = vm.dayData!!.events,
                                    durations = vm.dayData!!.durations,
                                    dailyPeakMetrics = vm.dayData!!.dailyPeakMetrics,
                                    liveMetrics = vm.liveData!!.liveSunMetrics
                                )
                            }
                            composable(Routes.Moon.route) {
                                Moon(
                                    currentTime = vm.currentTime,
                                    coordinates = coordinates!!,
                                    currentPosition = vm.liveData!!.currentMoonPosition,
                                    events = vm.dayData!!.lunarEvents,
                                    dailyPeakMetrics = vm.dayData!!.lunarDailyPeakMetrics!!,
                                    liveMetrics = vm.liveData!!.liveMoonMetrics
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