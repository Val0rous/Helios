package com.ephemeris.helios

import android.app.Application
import com.ephemeris.helios.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HeliosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@HeliosApplication)
            modules(appModule)
        }
    }
}
