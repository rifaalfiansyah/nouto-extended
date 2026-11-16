package org.kaorun.nouto.ui.utils

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.ItemNoteBinding

fun String?.toPlainText(): String =
    HtmlCompat.fromHtml(this.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

fun String.trimHtml(): String =
    replace(Regex("(<br\\s*/?>|\\s)+$"), "").trim()

fun ItemNoteBinding.applyNoteCardAppearance(note: Note) {
    if (note.isPinned && !note.isDeleted) {
        cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                MaterialColors.getColor(cardView, MaterialR.attr.colorSecondaryContainer)
            )
        )
        val pinnedTextColor = ColorStateList.valueOf(
            MaterialColors.getColor(noteTitle, MaterialR.attr.colorOnSecondaryContainer)
        )
        noteTitle.setTextColor(pinnedTextColor)
        noteTime.setTextColor(pinnedTextColor)
    } else {
        cardView.setCardBackgroundColor(
            ColorStateList.valueOf(
                MaterialColors.getColor(cardView, MaterialR.attr.colorSurfaceBright)
            )
        )
        noteTitle.setTextColor(
            ColorStateList.valueOf(
                MaterialColors.getColor(noteTitle, MaterialR.attr.colorOnSurface)
            )
        )
        noteTime.setTextColor(
            ColorStateList.valueOf(
                MaterialColors.getColor(noteTime, MaterialR.attr.colorOnSurfaceVariant)
            )
        )
    }
}
