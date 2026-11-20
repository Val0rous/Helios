package com.ephemeris.helios.di

import com.ephemeris.helios.utils.HeliosViewModel
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.location.AltitudeCorrector
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // App-wide singleton engines & repositories
    singleOf(::LocationDataStore)
    singleOf(::NativeGeocodingEngine)
    singleOf(::LocationService)
    singleOf(::AltitudeCorrector)

    // ViewModels (auto-wires constructor dependencies as you refactor HeliosViewModel)
    viewModelOf(::HeliosViewModel)
}
