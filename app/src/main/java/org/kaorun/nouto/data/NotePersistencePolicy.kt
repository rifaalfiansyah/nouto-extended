package org.kaorun.nouto.data

internal object NotePersistencePolicy {
    fun shouldDelete(note: Note): Boolean =
        note.title.isNullOrBlank() && note.content.isNullOrBlank()
}
