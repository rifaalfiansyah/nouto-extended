package org.kaorun.nouto.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.listitem.ListItemViewHolder
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.ItemNoteBinding
import org.kaorun.nouto.ui.utils.DateFormatUtils
import org.kaorun.nouto.ui.utils.applyNoteCardAppearance

class NoteExportAdapter(
    private val onItemClick: (Note) -> Unit
) : ListAdapter<Note, NoteExportAdapter.NoteExportViewHolder>(NoteDiffCallback()) {

    inner class NoteExportViewHolder(
        private val binding: ItemNoteBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var note: Note

        init {
            binding.cardView.isSwipeEnabled = false
            binding.revealLayout.isVisible = false
            binding.revealLayoutEnd.isVisible = false
            binding.cardView.setOnClickListener { onItemClick(note) }
        }

        fun bind(currentNote: Note) {
            note = currentNote
            val displayHtml = (if (note.title.isNullOrBlank()) note.content else note.title).orEmpty()
            binding.noteTitle.text = HtmlCompat.fromHtml(displayHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
            binding.noteTime.text = DateFormatUtils.formatNoteTime(binding.root.context, note.time)
            binding.applyNoteCardAppearance(note)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteExportViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteExportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteExportViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.bind(position, itemCount)
    }
}
