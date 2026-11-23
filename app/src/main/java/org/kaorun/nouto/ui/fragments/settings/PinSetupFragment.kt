package org.kaorun.nouto.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.kaorun.nouto.R
import org.kaorun.nouto.databinding.FragmentPinSetupBinding
import org.kaorun.nouto.ui.components.PinPadController
import org.kaorun.nouto.ui.components.Snackbars
import org.kaorun.nouto.ui.fragments.base.BaseFragment
import org.kaorun.nouto.ui.utils.AppLockManager
import org.kaorun.nouto.ui.utils.InsetsHandler

class PinSetupFragment : BaseFragment(R.layout.fragment_pin_setup) {
    companion object {
        const val MODE_SETUP = "setup"
        const val MODE_CHANGE = "change"
        const val MODE_DISABLE = "disable"
    }

    private enum class Step {
        VERIFY_CURRENT,
        ENTER_NEW,
        CONFIRM_NEW
    }

    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!
    private val args: PinSetupFragmentArgs by navArgs()

    private lateinit var pinPadController: PinPadController
    private var step: Step = Step.ENTER_NEW
    private var pendingNewPin: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        InsetsHandler.applyViewInsets(
            binding.appBarLayout,
            isTopPaddingEnabled = true,
            isBottomPaddingEnabled = false
        )
        InsetsHandler.applyViewInsets(
            binding.scrollView,
            isTopPaddingEnabled = false,
            isBottomPaddingEnabled = true
        )

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        pinPadController = PinPadController(
            binding = binding.pinLock,
            onPinComplete = ::handlePinEntered
        )
        pinPadController.setBiometricVisible(false)

        step = when (args.mode) {
            MODE_CHANGE, MODE_DISABLE -> Step.VERIFY_CURRENT
            else -> Step.ENTER_NEW
        }
        renderStep()
    }

    private fun renderStep(errorMessage: String? = null) {
        val (titleRes, subtitleRes) = when (step) {
            Step.VERIFY_CURRENT -> R.string.app_lock_verify_current_pin_title to
                R.string.app_lock_verify_current_pin_subtitle
            Step.ENTER_NEW -> if (args.mode == MODE_CHANGE) {
                R.string.app_lock_new_pin_title to R.string.app_lock_new_pin_subtitle
            } else {
                R.string.app_lock_create_pin_title to R.string.app_lock_create_pin_subtitle
            }
            Step.CONFIRM_NEW -> R.string.app_lock_confirm_pin_title to
                R.string.app_lock_confirm_pin_subtitle
        }

        pinPadController.setPrompt(
            title = getString(titleRes),
            subtitle = errorMessage ?: getString(subtitleRes),
            isError = errorMessage != null
        )
    }

    private fun handlePinEntered(pin: String) {
        when (step) {
            Step.VERIFY_CURRENT -> {
                if (AppLockManager.verifyPin(requireContext(), pin)) {
                    if (args.mode == MODE_DISABLE) {
                        AppLockManager.disableLock(requireContext())
                        showResultSnackbar(R.string.app_lock_disabled_message)
                        findNavController().popBackStack()
                    } else {
                        step = Step.ENTER_NEW
                        pinPadController.clear()
                        renderStep()
                    }
                } else {
                    pinPadController.clear(animateError = true)
                    renderStep(getString(R.string.app_lock_wrong_pin))
                }
            }
            Step.ENTER_NEW -> {
                pendingNewPin = pin
                step = Step.CONFIRM_NEW
                pinPadController.clear()
                renderStep()
            }
            Step.CONFIRM_NEW -> {
                if (pin == pendingNewPin) {
                    AppLockManager.savePin(requireContext(), pin)
                    val msgRes = if (args.mode == MODE_CHANGE) {
                        R.string.app_lock_pin_updated
                    } else {
                        R.string.app_lock_enabled_message
                    }
                    showResultSnackbar(msgRes)
                    findNavController().popBackStack()
                } else {
                    pendingNewPin = ""
                    step = Step.ENTER_NEW
                    pinPadController.clear(animateError = true)
                    renderStep(getString(R.string.app_lock_pin_mismatch))
                }
            }
        }
    }

    private fun showResultSnackbar(messageRes: Int) {
        val activityView = requireActivity().findViewById<View>(android.R.id.content)
        Snackbars.showSnackbarNoAction(
            view = activityView,
            anchorView = null,
            message = getString(messageRes)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
