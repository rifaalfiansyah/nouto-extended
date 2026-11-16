package org.kaorun.nouto.ui.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object ThemeHelper {
    const val KEY = "theme"

    fun apply(context: Context) {
        apply(PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, "system"))
    }

    fun apply(value: Any?) {
        AppCompatDelegate.setDefaultNightMode(
            when (value) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}