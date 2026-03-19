package com.ephemeris.helios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ephemeris.helios.ui.composables.Navbar
import com.ephemeris.helios.ui.screens.Sun
import com.ephemeris.helios.ui.theme.HeliosTheme
import com.ephemeris.helios.utils.Routes

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialStartDestination = intent.getStringExtra("startDestination") ?: Routes.Home.route
        setContent {
            navController = rememberNavController()
            var startDestination by remember { mutableStateOf(initialStartDestination) }
            HeliosTheme {
                Scaffold(
                    //modifier
                    //topBar
                    bottomBar = { Navbar(navController) },
                ) { paddingValues ->
                    LaunchedEffect(Unit) {
                        savedInstanceState?.getString("NAVIGATION_STATE")?.let { savedRoute ->
                            startDestination = savedRoute
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
                        composable(Routes.UV.route) {
                            //UV()
                        }
                        composable(Routes.Sun.route) {
                            Sun()
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