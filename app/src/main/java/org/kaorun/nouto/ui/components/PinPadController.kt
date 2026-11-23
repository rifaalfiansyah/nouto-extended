package org.kaorun.nouto.ui.components

import android.animation.ObjectAnimator
import android.R as AndroidR
import android.view.HapticFeedbackConstants
import android.view.animation.OvershootInterpolator
import androidx.core.view.isInvisible
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import org.kaorun.nouto.databinding.ViewPinLockBinding

class PinPadController(
    private val binding: ViewPinLockBinding,
    private val onPinComplete: (String) -> Unit,
    private val onBiometricClick: (() -> Unit)? = null
) {
    companion object {
        const val PIN_LENGTH = 4
    }

    private val enteredDigits = StringBuilder()
    private val dots = listOf(
        binding.pinDot1,
        binding.pinDot2,
        binding.pinDot3,
        binding.pinDot4
    )
    private val digitButtons = listOf(
        binding.pinKey0 to '0',
        binding.pinKey1 to '1',
        binding.pinKey2 to '2',
        binding.pinKey3 to '3',
        binding.pinKey4 to '4',
        binding.pinKey5 to '5',
        binding.pinKey6 to '6',
        binding.pinKey7 to '7',
        binding.pinKey8 to '8',
        binding.pinKey9 to '9'
    )
    private var defaultSubtitleColor: Int = MaterialColors.getColor(
        binding.pinSubtitle,
        MaterialR.attr.colorOnSurfaceVariant
    )
    private var isProcessing = false

    init {
        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                appendDigit(digit)
            }
        }
        binding.pinKeyBackspace.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            removeLastDigit()
        }
        binding.pinKeyBiometric.setOnClickListener {
            onBiometricClick?.invoke()
        }
        updateDots()
    }

    fun setPrompt(title: String, subtitle: String, isError: Boolean = false) {
        binding.pinTitle.text = title
        binding.pinSubtitle.text = subtitle
        binding.pinSubtitle.setTextColor(
            if (isError) {
                MaterialColors.getColor(binding.pinSubtitle, AndroidR.attr.colorError)
            } else {
                defaultSubtitleColor
            }
        )
    }

    fun setBiometricVisible(visible: Boolean) {
        binding.pinKeyBiometric.isInvisible = !visible || onBiometricClick == null
    }

    fun clear(animateError: Boolean = false) {
        isProcessing = false
        enteredDigits.clear()
        updateDots()
        if (animateError) {
            binding.pinDotsContainer.performHapticFeedback(HapticFeedbackConstants.REJECT)
            ObjectAnimator.ofFloat(
                binding.pinDotsContainer,
                "translationX",
                0f, -24f, 24f, -18f, 18f, -8f, 8f, 0f
            ).apply {
                duration = 380L
                start()
            }
        }
    }

    private fun appendDigit(digit: Char) {
        if (isProcessing || enteredDigits.length >= PIN_LENGTH) return
        enteredDigits.append(digit)
        val filledIndex = enteredDigits.length - 1
        updateDots(animatedIndex = filledIndex)

        if (enteredDigits.length == PIN_LENGTH) {
            isProcessing = true
            val pin = enteredDigits.toString()
            binding.root.postDelayed({
                onPinComplete(pin)
            }, 90L)
        }
    }

    private fun removeLastDigit() {
        if (isProcessing || enteredDigits.isEmpty()) return
        enteredDigits.deleteCharAt(enteredDigits.length - 1)
        updateDots()
    }

    private fun updateDots(animatedIndex: Int = -1) {
        dots.forEachIndexed { index, dot ->
            val selected = index < enteredDigits.length
            dot.isSelected = selected
            if (index == animatedIndex && selected) {
                dot.scaleX = 0.7f
                dot.scaleY = 0.7f
                dot.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(120L)
                    .setInterpolator(OvershootInterpolator(2f))
                    .withEndAction {
                        dot.animate().scaleX(1f).scaleY(1f).setDuration(80L).start()
                    }
                    .start()
            }
        }
    }
}
