package org.kaorun.nouto.ui.fragments.settings.preferences

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import org.kaorun.nouto.R
import org.kaorun.nouto.ui.components.MaterialListPreference
import org.kaorun.nouto.ui.components.MaterialSwitchPreferenceCompat
import org.kaorun.nouto.ui.fragments.base.PreferenceBaseFragment
import org.kaorun.nouto.ui.fragments.settings.PinSetupFragment
import org.kaorun.nouto.ui.fragments.settings.SettingsAppLockFragmentDirections
import org.kaorun.nouto.ui.utils.AppLockManager

class PreferenceAppLockFragment : PreferenceBaseFragment() {
    private var enableSwitch: MaterialSwitchPreferenceCompat? = null
    private var changePinPref: Preference? = null
    private var biometricSwitch: MaterialSwitchPreferenceCompat? = null
    private var timeoutPref: MaterialListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_app_lock, rootKey)

        enableSwitch = findPreference(AppLockManager.KEY_ENABLED)
        changePinPref = findPreference("app_lock_change_pin")
        biometricSwitch = findPreference(AppLockManager.KEY_BIOMETRIC)
        timeoutPref = findPreference(AppLockManager.KEY_TIMEOUT)

        enableSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val targetEnabled = newValue as Boolean
            val mode = if (targetEnabled) PinSetupFragment.MODE_SETUP else PinSetupFragment.MODE_DISABLE
            navigateToPinSetup(mode)
            false
        }

        changePinPref?.setOnPreferenceClickListener {
            navigateToPinSetup(PinSetupFragment.MODE_CHANGE)
            true
        }

        biometricSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val targetEnabled = newValue as Boolean
            if (targetEnabled) {
                promptBiometricConfirmation()
                false
            } else {
                AppLockManager.setBiometricEnabled(requireContext(), false)
                true
            }
        }

        timeoutPref?.setOnPreferenceChangeListener { _, newValue ->
            val valueStr = newValue.toString()
            AppLockManager.setLockTimeoutValue(requireContext(), valueStr)
            timeoutPref?.value = valueStr
            true
        }

        syncPreferences()
    }

    override fun onResume() {
        super.onResume()
        syncPreferences()
    }

    private fun syncPreferences() {
        val context = context ?: return
        val lockEnabled = AppLockManager.isLockEnabled(context)
        val biometricAvailable = AppLockManager.isBiometricAvailable(context)

        enableSwitch?.isChecked = lockEnabled
        changePinPref?.isEnabled = lockEnabled

        biometricSwitch?.apply {
            isEnabled = lockEnabled && biometricAvailable
            isChecked = lockEnabled && AppLockManager.isBiometricEnabled(context)
            summary = if (biometricAvailable) {
                getString(R.string.app_lock_biometric_summary)
            } else {
                getString(R.string.app_lock_biometric_unavailable)
            }
        }

        timeoutPref?.apply {
            isEnabled = lockEnabled
            value = AppLockManager.getLockTimeoutValue(context)
        }
    }

    private fun navigateToPinSetup(mode: String) {
        requireParentFragment().findNavController().navigate(
            SettingsAppLockFragmentDirections.actionSettingsAppLockFragmentToPinSetupFragment(mode)
        )
    }

    private fun promptBiometricConfirmation() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val ctx = context ?: return
                    AppLockManager.setBiometricEnabled(ctx, true)
                    biometricSwitch?.isChecked = true
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_biometric))
            .setSubtitle(getString(R.string.app_lock_biometric_enable_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .build()
        prompt.authenticate(promptInfo)
    }
}
