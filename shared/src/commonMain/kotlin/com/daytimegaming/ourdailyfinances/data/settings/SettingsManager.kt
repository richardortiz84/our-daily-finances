package com.daytimegaming.ourdailyfinances.data.settings

interface SettingsManager {
    fun isStaging(): Boolean
    fun setStaging(enabled: Boolean)
}
