package org.kaorun.nouto

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.kaorun.nouto.ui.utils.ThemeHelper

class NoutoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        ThemeHelper.apply(this)
    }
}