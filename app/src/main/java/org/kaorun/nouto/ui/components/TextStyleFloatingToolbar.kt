package org.kaorun.nouto.ui.components

import android.view.View
import android.view.View.AccessibilityDelegate
import android.view.accessibility.AccessibilityEvent
import com.google.android.material.button.MaterialButton
import com.onegravity.rteditor.RTEditText
import com.onegravity.rteditor.RTManager
import com.onegravity.rteditor.effects.Effect
import com.onegravity.rteditor.effects.Effects
import com.onegravity.rteditor.spans.BoldSpan
import com.onegravity.rteditor.spans.BulletSpan
import com.onegravity.rteditor.spans.ItalicSpan
import com.onegravity.rteditor.spans.NumberSpan
import com.onegravity.rteditor.spans.RTSpan
import com.onegravity.rteditor.spans.StrikethroughSpan
import com.onegravity.rteditor.spans.UnderlineSpan

class TextStyleFloatingToolbar(
    private val rtManager: RTManager,
    private val actionUndo: MaterialButton,
    private val actionRedo: MaterialButton,
    private val styleBold: MaterialButton,
    private val styleItalic: MaterialButton,
    private val styleUnderline: MaterialButton,
    private val styleStrikethrough: MaterialButton,
    private val styleBulletList: MaterialButton,
    private val styleNumberedList: MaterialButton,
    private val fields: List<RTEditText>,
    private val onFormatChanged: (() -> Unit)? = null
) {
    fun setupFloatingToolbar() {
        actionUndo.setOnClickListener {
            rtManager.onUndo()
            updateActiveFieldButtons()
            onFormatChanged?.invoke()
        }
        actionRedo.setOnClickListener {
            rtManager.onRedo()
            updateActiveFieldButtons()
            onFormatChanged?.invoke()
        }

        setupStyleButton(styleBold, Effects.BOLD)
        setupStyleButton(styleItalic, Effects.ITALIC)
        setupStyleButton(styleUnderline, Effects.UNDERLINE)
        setupStyleButton(styleStrikethrough, Effects.STRIKETHROUGH)

        styleBulletList.setOnClickListener {
            val isActive = styleBulletList.isChecked
            if (isActive) {
                styleNumberedList.isChecked = false
            }
            rtManager.onEffectSelected(Effects.BULLET, isActive)
            updateActiveFieldButtons()
        }

        styleNumberedList.setOnClickListener {
            val isActive = styleNumberedList.isChecked
            if (isActive) {
                styleBulletList.isChecked = false
            }
            rtManager.onEffectSelected(Effects.NUMBER, isActive)
            updateActiveFieldButtons()
        }

        fields.forEach { setupTextClassifier(it) }
    }

    private fun setupStyleButton(
        button: MaterialButton,
        effect: Effect<Boolean, out RTSpan<Boolean>>
    ) {
        button.setOnClickListener {
            val isActive = button.isChecked
            rtManager.onEffectSelected(effect, isActive)
        }
    }

    private fun setupTextClassifier(textbox: RTEditText) {
        textbox.accessibilityDelegate = object : AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                ) {
                    updateButtonsForSelection(textbox)
                }
            }
        }
    }

    private fun updateActiveFieldButtons() {
        val activeField = fields.firstOrNull { it.hasFocus() } ?: fields.firstOrNull() ?: return
        updateButtonsForSelection(activeField)
    }

    private fun updateButtonsForSelection(textbox: RTEditText) {
        val spannable = textbox.text ?: return
        val rawStart = textbox.selectionStart.coerceAtLeast(0)
        val rawEnd = textbox.selectionEnd.coerceAtLeast(0)
        val start = minOf(rawStart, rawEnd)
        val end = maxOf(rawStart, rawEnd)

        styleBold.isChecked = spannable.getSpans(start, end, BoldSpan::class.java).isNotEmpty()
        styleItalic.isChecked = spannable.getSpans(start, end, ItalicSpan::class.java).isNotEmpty()
        styleUnderline.isChecked = spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
        styleStrikethrough.isChecked = spannable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()
        styleBulletList.isChecked = spannable.getSpans(start, end, BulletSpan::class.java).isNotEmpty()
        styleNumberedList.isChecked = spannable.getSpans(start, end, NumberSpan::class.java).isNotEmpty()
    }
}
