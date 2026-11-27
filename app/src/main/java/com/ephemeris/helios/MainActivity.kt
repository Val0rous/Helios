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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ephemeris.helios.ui.composables.Navbar
import com.ephemeris.helios.ui.composables.NavbarItems
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
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialStartDestination = savedInstanceState?.getString("NAVIGATION_STATE")
            ?: intent.getStringExtra("startDestination")
            ?: Routes.Home.route

        setContent {
            val homePageIndex = remember {
                NavbarItems.items.indexOfFirst { it.route == Routes.Home.route }.coerceAtLeast(0)
            }
            val initialPage = remember(initialStartDestination) {
                NavbarItems.items.indexOfFirst { it.route == initialStartDestination }.coerceAtLeast(0)
            }
            val pagerState = rememberPagerState(initialPage = initialPage) { NavbarItems.items.size }
            val coroutineScope = rememberCoroutineScope()

            val coordinates by vm.coordinates.collectAsStateWithLifecycle()
            val isDataStoreLoaded by vm.isDataStoreLoaded.collectAsStateWithLifecycle()
            val locationStatus by vm.locationStatus.collectAsStateWithLifecycle()
            val isTracking by vm.isContinuousGPSTrackingEnabled.collectAsStateWithLifecycle()
            val currentTime by vm.currentTime.collectAsStateWithLifecycle()
            val isAutoUpdateEnabled by vm.isAutoUpdateEnabled.collectAsStateWithLifecycle()
            val isEphemerisReady by vm.isEphemerisReady.collectAsStateWithLifecycle()
            val coordinateFormat by vm.coordinateFormat.collectAsStateWithLifecycle()
            val customTimezoneId by vm.customTimezoneId.collectAsStateWithLifecycle()
            val atmosphericConditions by vm.atmosphericConditions.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            // Intercept back on non-Home navbar tabs without predictive-back resizing,
            // returning directly to Home. When on Home, disabled so system back runs normally.
            val navigationState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(
                state = navigationState,
                isBackEnabled = pagerState.currentPage != homePageIndex,
                onBackCompleted = {
                    coroutineScope.launch {
                        pagerState.scrollToPage(homePageIndex)
                    }
                },
            )

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
                                coordinateFormat = coordinateFormat,
                                onToggleCoordinateFormat = vm::toggleCoordinateFormat,
                                customTimezoneId = customTimezoneId,
                                onSelectCustomTimezone = vm::setCustomTimezoneId,
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
                                atmosphericConditions = atmosphericConditions,
                                onAtmosphericConditionsChange = vm::setAtmosphericConditions,
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
                                    onResetToNow = vm::resetToNow,
                                )
                                Navbar(
                                    selectedIndex = pagerState.currentPage,
                                    onSelectTab = { index ->
                                        coroutineScope.launch {
                                            if (abs(index - pagerState.currentPage) <= 1) {
                                                pagerState.animateScrollToPage(index)
                                            } else {
                                                pagerState.scrollToPage(index)
                                            }
                                        }
                                    },
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) { paddingValues ->
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

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues = paddingValues),
                        ) { page ->
                            val childOwner = rememberNavigationEventDispatcherOwner(
                                enabled = pagerState.currentPage == page,
                            )
                            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
                                when (NavbarItems.items[page].route) {
                                    Routes.Home.route -> Home()
                                    Routes.Exposure.route -> {
                                        //UV()
                                    }
                                    Routes.Sun.route -> Sun()
                                    Routes.Moon.route -> Moon()
                                    Routes.Maps.route -> Maps()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
