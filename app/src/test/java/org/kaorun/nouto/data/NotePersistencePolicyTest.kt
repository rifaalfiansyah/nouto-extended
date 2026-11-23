package org.kaorun.nouto.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePersistencePolicyTest {
    @Test
    fun deletesOnlyWhenBothFieldsAreBlank() {
        assertTrue(NotePersistencePolicy.shouldDelete(Note(title = "  ", content = null, time = 1L)))
        assertTrue(NotePersistencePolicy.shouldDelete(Note(title = null, content = "\n", time = 1L)))
        assertFalse(NotePersistencePolicy.shouldDelete(Note(title = "Title", content = "", time = 1L)))
        assertFalse(NotePersistencePolicy.shouldDelete(Note(title = null, content = "Text", time = 1L)))
    }
}
