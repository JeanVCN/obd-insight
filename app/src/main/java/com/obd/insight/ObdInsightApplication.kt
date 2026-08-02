package com.obd.insight

import android.app.Application
import com.obd.insight.di.AppModule

class ObdInsightApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppModule.initialize(this)
    }
}
