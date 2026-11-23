package org.kaorun.nouto.ui.components

import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroupOverlay
import android.view.View.MeasureSpec
import androidx.appcompat.widget.PopupMenu
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Note

object NoteActionsMenu {
    fun show(
        anchor: View,
        note: Note,
        onPin: (Note) -> Unit,
        onExport: (Note) -> Unit,
        onDelete: (Note) -> Unit,
        gapBelowAnchorDp: Int = 0
    ) {
        val offsetAnchor = createOffsetAnchor(anchor, gapBelowAnchorDp)
        val popupAnchor = offsetAnchor?.first ?: anchor
        val popup = PopupMenu(
            ContextThemeWrapper(anchor.context, R.style.ThemeOverlay_Popup),
            popupAnchor,
            Gravity.START
        )
        popup.menuInflater.inflate(R.menu.appbar, popup.menu)
        popup.menu.findItem(R.id.pin).apply {
            title = anchor.context.getString(if (note.isPinned) R.string.unpin else R.string.pin)
            setIcon(if (note.isPinned) R.drawable.keep_off_24px else R.drawable.keep_24px)
        }
        popup.setForceShowIcon(true)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.pin -> {
                    onPin(note)
                    true
                }
                R.id.export -> {
                    onExport(note)
                    true
                }
                R.id.delete -> {
                    onDelete(note)
                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener {
            offsetAnchor?.second?.remove(offsetAnchor.first)
        }
        popup.show()
    }

    private fun createOffsetAnchor(
        anchor: View,
        gapBelowAnchorDp: Int
    ): Pair<View, ViewGroupOverlay>? {
        if (gapBelowAnchorDp <= 0 || anchor.width == 0 || anchor.height == 0) return null
        val overlayHost = anchor.rootView as? ViewGroup ?: return null
        val anchorLocation = IntArray(2)
        val hostLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        overlayHost.getLocationOnScreen(hostLocation)

        val gapPx = (gapBelowAnchorDp * anchor.resources.displayMetrics.density).toInt()
        val left = anchorLocation[0] - hostLocation[0]
        val top = anchorLocation[1] - hostLocation[1] + anchor.height + gapPx
        val gapAnchor = View(anchor.context).apply {
            alpha = 0f
            layoutParams = ViewGroup.LayoutParams(1, 1)
            measure(
                MeasureSpec.makeMeasureSpec(1, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(1, MeasureSpec.EXACTLY)
            )
            layout(left, top, left + 1, top + 1)
        }
        val overlay = overlayHost.overlay
        overlay.add(gapAnchor)
        return gapAnchor to overlay
    }
}
