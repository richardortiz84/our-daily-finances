package com.daytimegaming.ourdailyfinances.data.settings

import java.util.prefs.Preferences

class JvmSettingsManager : SettingsManager {
    private val prefs = Preferences.userNodeForPackage(JvmSettingsManager::class.java)

    override fun isStaging(): Boolean {
        return prefs.getBoolean("staging", true)
    }

    override fun setStaging(enabled: Boolean) {
        prefs.putBoolean("staging", enabled)
    }
}
