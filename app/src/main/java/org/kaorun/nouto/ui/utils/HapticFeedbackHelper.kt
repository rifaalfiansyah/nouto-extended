package org.kaorun.nouto.ui.utils

import android.view.View
import androidx.preference.PreferenceManager
import org.kaorun.nouto.data.PreferenceAppearanceKeys

object HapticFeedbackHelper {
    fun perform(view: View, feedbackConstant: Int) {
        val hapticsEnabled = PreferenceManager
            .getDefaultSharedPreferences(view.context)
            .getBoolean(PreferenceAppearanceKeys.IS_HAPTIC_FEEDBACK_ENABLED, true)
        if (hapticsEnabled) {
            view.performHapticFeedback(feedbackConstant)
        }
    }
}
