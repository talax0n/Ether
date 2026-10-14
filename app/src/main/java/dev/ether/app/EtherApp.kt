package dev.ether.app

import android.app.Application
import dev.ether.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class EtherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EtherApp)
            modules(appModules)
        }
    }
}
