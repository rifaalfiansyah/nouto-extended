package org.kaorun.nouto.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import org.kaorun.nouto.R
import org.kaorun.nouto.databinding.ActivityMainBinding
import org.kaorun.nouto.ui.components.PinPadController
import org.kaorun.nouto.ui.utils.AppLockManager
import org.kaorun.nouto.ui.utils.BlackThemeHelper
import org.kaorun.nouto.ui.utils.ColorThemeHelper
import org.kaorun.nouto.ui.utils.InsetsHandler

class MainActivity : AppCompatActivity() {
    lateinit var roomBackup: RoomBackup
    private lateinit var binding: ActivityMainBinding
    private lateinit var pinPadController: PinPadController
    private var hasPromptedBiometricForSession = false

    private val lockedBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            moveTaskToBack(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorThemeHelper.apply(this)
        BlackThemeHelper.apply(this)
        roomBackup = RoomBackup(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        InsetsHandler.applyViewInsets(binding.lockOverlay)
        onBackPressedDispatcher.addCallback(this, lockedBackCallback)

        pinPadController = PinPadController(
            binding = binding.lockPinView,
            onPinComplete = ::handlePinUnlock,
            onBiometricClick = ::showBiometricPrompt
        )

        if (savedInstanceState == null && AppLockManager.isLockEnabled(this)) {
            updateLockScreenState(autoTriggerBiometric = false)
        }
    }

    override fun onStart() {
        super.onStart()
        AppLockManager.onAppForegrounded(this)
        updateLockScreenState(autoTriggerBiometric = true)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            AppLockManager.onAppBackgrounded()
            if (AppLockManager.isLockEnabled(this)) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    private fun updateLockScreenState(autoTriggerBiometric: Boolean) {
        val shouldLock = AppLockManager.shouldShowLockScreen(this)
        lockedBackCallback.isEnabled = shouldLock

        if (shouldLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            hideKeyboard()
            val biometricEnabled = AppLockManager.isBiometricEnabled(this)
            pinPadController.setBiometricVisible(biometricEnabled)
            pinPadController.clear()
            pinPadController.setPrompt(
                title = getString(R.string.app_lock_locked_title),
                subtitle = getString(
                    if (biometricEnabled) R.string.app_lock_enter_pin_or_biometric
                    else R.string.app_lock_enter_pin
                )
            )
            binding.navHostFragmentContainer.isInvisible = true
            binding.lockOverlay.animate().cancel()
            binding.lockOverlay.alpha = 1f
            binding.lockOverlay.isVisible = true

            if (autoTriggerBiometric && biometricEnabled && !hasPromptedBiometricForSession) {
                hasPromptedBiometricForSession = true
                binding.lockOverlay.post {
                    if (!isFinishing && !isDestroyed && AppLockManager.shouldShowLockScreen(this)) {
                        showBiometricPrompt()
                    }
                }
            }
        } else {
            hasPromptedBiometricForSession = false
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            binding.navHostFragmentContainer.isInvisible = false
            binding.lockOverlay.isVisible = false
        }
    }

    private fun handlePinUnlock(pin: String) {
        if (AppLockManager.verifyPin(this, pin)) {
            unlockApp()
        } else {
            pinPadController.clear(animateError = true)
            pinPadController.setPrompt(
                title = getString(R.string.app_lock_locked_title),
                subtitle = getString(R.string.app_lock_wrong_pin),
                isError = true
            )
        }
    }

    private fun showBiometricPrompt() {
        if (!AppLockManager.isBiometricEnabled(this)) return
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_lock_biometric_prompt_title))
            .setSubtitle(getString(R.string.app_lock_biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.app_lock_biometric_use_pin))
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun unlockApp() {
        AppLockManager.markUnlocked()
        hasPromptedBiometricForSession = false
        lockedBackCallback.isEnabled = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding.navHostFragmentContainer.isInvisible = false
        binding.lockOverlay.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction {
                binding.lockOverlay.isVisible = false
                binding.lockOverlay.alpha = 1f
                pinPadController.clear()
            }
            .start()
    }

    private fun hideKeyboard() {
        val view: View = currentFocus ?: binding.root
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
