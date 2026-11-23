package org.kaorun.nouto.ui.components

import android.view.ContextThemeWrapper
import android.view.View
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
        onOpen: (() -> Unit)? = null
    ) {
        val popup = PopupMenu(ContextThemeWrapper(anchor.context, R.style.ThemeOverlay_Popup), anchor)
        popup.menuInflater.inflate(R.menu.appbar, popup.menu)
        popup.menu.findItem(R.id.open_note).isVisible = onOpen != null
        popup.menu.findItem(R.id.pin).apply {
            title = anchor.context.getString(if (note.isPinned) R.string.unpin else R.string.pin)
            setIcon(if (note.isPinned) R.drawable.keep_off_24px else R.drawable.keep_24px)
        }
        popup.setForceShowIcon(true)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.open_note -> {
                    val openAction = onOpen ?: return@setOnMenuItemClickListener false
                    openAction()
                    true
                }
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
        popup.show()
    }
}
