package org.kaorun.nouto.ui.fragments.settings.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.preference.Preference
import org.kaorun.nouto.BuildConfig
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment

class PreferenceAboutFragment : PreferenceBaseFragment() {
    companion object {
        const val VERSION_NAME = BuildConfig.VERSION_NAME
    }
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.preferences_about, rootKey)

        findPreference<Preference>("source_code")?.setOnPreferenceClickListener {
            openUrl(R.string.repository_link)
            true
        }

        findPreference<Preference>("original_project")?.setOnPreferenceClickListener {
            openUrl(R.string.original_repository_link)
            true
        }

        findPreference<Preference>("version")?.summary = "v$VERSION_NAME"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageView = LayoutInflater.from(requireContext())
            .inflate(R.layout.illustration_about_application, listView, false)
        super.onViewCreated(view, savedInstanceState)
    }
}