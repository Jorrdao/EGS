package storm.os

import android.app.Application

class StormOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppConfig.context = applicationContext
    }
}