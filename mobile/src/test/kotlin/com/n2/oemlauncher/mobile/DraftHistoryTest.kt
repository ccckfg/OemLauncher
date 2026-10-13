package com.n2.oemlauncher.mobile

import com.n2.oemlauncher.mobile.editor.DraftHistory
import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.WatchConfigState
import org.junit.Assert.*
import org.junit.Test

class DraftHistoryTest {
    private val key = "com.example.app/com.example.app.Main"

    @Test fun editsUndoRedoAndBranching() {
        val initial = DraftHistory(LauncherConfig())
        val edited = initial.edit { ConfigEditor.toggleFavorite(it, key).first }
            .edit { ConfigEditor.setHidden(it, key, true) }
        assertTrue(edited.dirty)
        assertTrue(key in edited.current.hidden)
        assertTrue(key in edited.undo().current.favorites)
        assertEquals(edited.current, edited.undo().redo().current)
        val branch = edited.undo().edit { ConfigEditor.setSortMode(it, com.n2.oemlauncher.shared.model.SortMode.ALPHABETICAL) }
        assertTrue(branch.redo.isEmpty())
    }

    @Test fun noOpDoesNotAddHistory() {
        val initial = DraftHistory(LauncherConfig())
        assertEquals(initial, initial.edit { ConfigEditor.setHidden(it, key, false) })
        assertEquals(initial, initial.undo())
        assertEquals(initial, initial.redo())
    }

    @Test fun returningToBaselineClearsDirty() {
        val initial = DraftHistory(LauncherConfig())
        assertFalse(initial.edit { ConfigEditor.toggleFavorite(it, key).first }.undo().dirty)
    }

    @Test fun commitExceedsEveryKnownRevision() {
        val initial = DraftHistory(LauncherConfig(revision = 8))
        val state = WatchConfigState(LauncherConfig(revision = 12), lastDesiredRevision = 15)
        val outgoing = initial.commit(state, 20, 123)
        assertEquals(21L, outgoing.revision)
        assertEquals(ConfigOrigin.PHONE, outgoing.origin)
        assertEquals(123L, outgoing.editedAtMillis)
        assertEquals(8L, initial.current.revision)
    }

    @Test fun acknowledgementKeepsEditsMadeWhileWaiting() {
        val initial = DraftHistory(LauncherConfig())
        val sent = initial.commit(null, null, 5)
        val changed = initial.edit { ConfigEditor.updateLayout(it) { layout -> layout.copy(mode = LayoutMode.GRID) } }
        val acknowledged = changed.acknowledge(sent, sent.revision)
        assertTrue(acknowledged.dirty)
        assertEquals(LayoutMode.GRID, acknowledged.current.layout.mode)
        assertEquals(sent.revision, acknowledged.baseRevision)
    }
}
