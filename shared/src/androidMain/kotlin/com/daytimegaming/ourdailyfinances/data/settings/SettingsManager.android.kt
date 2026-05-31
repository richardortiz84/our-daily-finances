package com.daytimegaming.ourdailyfinances.data.settings

import android.content.Context

object AndroidContextProvider {
    lateinit var context: Context
}

class AndroidSettingsManager : SettingsManager {
    private val prefs by lazy {
        AndroidContextProvider.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    override fun isStaging(): Boolean {
        return prefs.getBoolean("staging", true)
    }

    override fun setStaging(enabled: Boolean) {
        prefs.edit().putBoolean("staging", enabled).apply()
    }
}
