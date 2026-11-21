package com.ephemeris.helios.di

import com.ephemeris.helios.data.EphemerisRepository
import com.ephemeris.helios.data.LocationRepository
import com.ephemeris.helios.data.TimeRepository
import com.ephemeris.helios.ui.theme.MaterialColorEngine
import com.ephemeris.helios.ui.viewmodels.HomeViewModel
import com.ephemeris.helios.ui.viewmodels.MainViewModel
import com.ephemeris.helios.ui.viewmodels.MapsViewModel
import com.ephemeris.helios.ui.viewmodels.MoonViewModel
import com.ephemeris.helios.ui.viewmodels.SunViewModel
import com.ephemeris.helios.utils.LocationService
import com.ephemeris.helios.utils.datastore.LocationDataStore
import com.ephemeris.helios.utils.location.AltitudeCorrector
import com.ephemeris.helios.utils.location.NativeGeocodingEngine
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Low-level singleton services & engines
    singleOf(::LocationDataStore)
    singleOf(::NativeGeocodingEngine)
    singleOf(::LocationService)
    singleOf(::AltitudeCorrector)
    singleOf(::MaterialColorEngine)

    // Shared app-wide singleton repositories
    singleOf(::LocationRepository)
    singleOf(::TimeRepository)
    singleOf(::EphemerisRepository)

    // Focused ViewModels (shared shell + per-screen)
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SunViewModel)
    viewModelOf(::MoonViewModel)
    viewModelOf(::MapsViewModel)
}
