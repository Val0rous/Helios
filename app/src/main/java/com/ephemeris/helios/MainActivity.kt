package com.ephemeris.helios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ephemeris.helios.ui.composables.Navbar
import com.ephemeris.helios.ui.composables.TimeMachine
import com.ephemeris.helios.ui.composables.TopBar
import com.ephemeris.helios.ui.screens.Home
import com.ephemeris.helios.ui.screens.Maps
import com.ephemeris.helios.ui.screens.Moon
import com.ephemeris.helios.ui.screens.Sun
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.ui.viewmodels.MainViewModel
import com.ephemeris.helios.utils.LocationPermissionWrapper
import com.ephemeris.helios.utils.Routes
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private val vm: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialStartDestination = intent.getStringExtra("startDestination") ?: Routes.Home.route

        setContent {
            navController = rememberNavController()
            var startDestination by remember { mutableStateOf(initialStartDestination) }
            val coordinates by vm.coordinates.collectAsStateWithLifecycle()
            val isDataStoreLoaded by vm.isDataStoreLoaded.collectAsStateWithLifecycle()
            val locationStatus by vm.locationStatus.collectAsStateWithLifecycle()
            val isTracking by vm.isContinuousGPSTrackingEnabled.collectAsStateWithLifecycle()
            val currentTime by vm.currentTime.collectAsStateWithLifecycle()
            val isAutoUpdateEnabled by vm.isAutoUpdateEnabled.collectAsStateWithLifecycle()
            val isEphemerisReady by vm.isEphemerisReady.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            HeliosTheme {
                // Wrapper handles all dialogs and lifecycle automatically
                LocationPermissionWrapper(
                    snackbarHostState = snackbarHostState,
                    onLocationRequestFailed = vm::onLocationRequestFailed,
                ) { requestOneOffLocation, startContinuousTracking, stopTracking ->

                    Scaffold(
                        topBar = {
                            TopBar(
                                currentTime = currentTime,
                                coordinates = coordinates,
                                locationStatus = locationStatus,
                                onSaveCoordinates = vm::saveCoordinates,
                                onLocationClick = {
                                    vm.onLocationRequestStarted()
                                    requestOneOffLocation()
                                },
                                isTracking = isTracking,
                                onToggleTracking = { enableTracking ->
                                    vm.setContinuousTracking(enableTracking)
                                    if (enableTracking) {
                                        startContinuousTracking(10000L)
                                    } else {
                                        stopTracking()
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TimeMachine(
                                    time = currentTime,
                                    coordinates = coordinates,
                                    isAutoUpdate = isAutoUpdateEnabled,
                                    onTimeChange = vm::setTime,
                                    onAutoUpdateChange = vm::setAutoUpdateEnabled,
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
                        if (!isDataStoreLoaded) {
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
                        if (!isEphemerisReady) {
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
                            composable(Routes.Home.route) { Home() }
                            composable(Routes.Exposure.route) {
                                //UV()
                            }
                            composable(Routes.Sun.route) { Sun() }
                            composable(Routes.Moon.route) { Moon() }
                            composable(Routes.Maps.route) { Maps() }
                        }
                    }
                }
            }
        }
    }
}
