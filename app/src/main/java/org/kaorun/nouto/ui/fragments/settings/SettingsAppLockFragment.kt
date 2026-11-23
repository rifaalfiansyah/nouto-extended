package org.kaorun.nouto.ui.fragments.settings

import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.SettingsBaseFragment
import org.kaorun.nouto.ui.fragments.settings.preferences.PreferenceAppLockFragment

class SettingsAppLockFragment : SettingsBaseFragment() {
    override val titleRes = R.string.app_lock
    override fun preferenceFragment() = PreferenceAppLockFragment()
}
