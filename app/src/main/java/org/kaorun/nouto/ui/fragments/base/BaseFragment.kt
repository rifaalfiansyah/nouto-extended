package org.kaorun.nouto.ui.fragments.base

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.R
import com.google.android.material.transition.MaterialSharedAxis

@SuppressLint("PrivateResource")
abstract class BaseFragment(contentLayoutId: Int) : Fragment(contentLayoutId) {
    private val durationEmphasized by lazy {
        resources.getInteger(R.integer.m3_sys_motion_duration_long2).toLong()
    }

    private val interpolatorEmphasized by lazy {
        AnimationUtils.loadInterpolator(
            requireContext(),
            R.interpolator.m3_sys_motion_easing_emphasized
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = createSharedAxis(forward = true)
        returnTransition = createSharedAxis(forward = false)
        exitTransition = createSharedAxis(forward = true)
        reenterTransition = createSharedAxis(forward = false)
    }

    private fun createSharedAxis(forward: Boolean) =
        MaterialSharedAxis(MaterialSharedAxis.X, forward).apply {
            duration = durationEmphasized
            interpolator = interpolatorEmphasized
        }

    protected fun createTypeface(): Typeface {
        val font = ResourcesCompat.getFont(
            requireContext(),
            org.kaorun.nouto.R.font.google_sans_flex_round
        )
        return Typeface.create(font, 500, false)
    }
}
