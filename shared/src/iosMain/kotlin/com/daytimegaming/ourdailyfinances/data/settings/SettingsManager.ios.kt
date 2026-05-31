package com.daytimegaming.ourdailyfinances.data.settings

import platform.Foundation.NSUserDefaults

class IosSettingsManager : SettingsManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun isStaging(): Boolean {
        if (defaults.objectForKey("staging") == null) {
            return true
        }
        return defaults.boolForKey("staging")
    }

    override fun setStaging(enabled: Boolean) {
        defaults.setBool(enabled, "staging")
    }
}
