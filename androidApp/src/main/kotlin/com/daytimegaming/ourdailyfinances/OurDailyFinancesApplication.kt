package com.daytimegaming.ourdailyfinances

import android.app.Application
import com.daytimegaming.ourdailyfinances.data.settings.AndroidContextProvider
import com.daytimegaming.ourdailyfinances.di.initKoin
import com.google.firebase.FirebaseApp

class OurDailyFinancesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AndroidContextProvider.context = this

        FirebaseApp.initializeApp(this)

        initKoin(enableHttpLogging = true)
    }
}
