package com.n2.oemlauncher.mobile.editor

import com.n2.oemlauncher.shared.config.ConfigEditor
import com.n2.oemlauncher.shared.model.ConfigOrigin
import com.n2.oemlauncher.shared.model.LauncherConfig
import com.n2.oemlauncher.shared.model.WatchConfigState
import com.n2.oemlauncher.shared.protocol.RevisionPolicy
import kotlinx.serialization.Serializable

@Serializable
data class DraftHistory(
    val current: LauncherConfig,
    val baseline: LauncherConfig = current,
    val baseRevision: Long = current.revision,
    val undo: List<LauncherConfig> = emptyList(),
    val redo: List<LauncherConfig> = emptyList(),
) {
    val dirty: Boolean get() = !sameContent(current, baseline)

    fun edit(transform: (LauncherConfig) -> LauncherConfig): DraftHistory {
        val updated = transform(current)
        if (sameContent(updated, current)) return this
        return copy(
            current = updated,
            undo = undo + current,
            redo = emptyList(),
        )
    }

    fun undo(): DraftHistory = if (undo.isEmpty()) this else copy(
        current = undo.last(), undo = undo.dropLast(1), redo = redo + current,
    )

    fun redo(): DraftHistory = if (redo.isEmpty()) this else copy(
        current = redo.last(), redo = redo.dropLast(1), undo = undo + current,
    )

    fun commit(state: WatchConfigState?, lastSent: Long?, now: Long): LauncherConfig =
        ConfigEditor.stamp(
            current,
            RevisionPolicy.next(
                baseRevision, state?.current?.revision ?: 0,
                state?.lastDesiredRevision ?: 0, lastSent ?: 0,
            ),
            ConfigOrigin.PHONE,
            now,
        )

    fun acknowledge(sent: LauncherConfig, watchRevision: Long): DraftHistory = copy(
        baseline = sent, baseRevision = watchRevision,
    )

    companion object {
        fun sameContent(a: LauncherConfig, b: LauncherConfig): Boolean =
            ConfigEditor.stamp(a, 0, ConfigOrigin.WATCH, 0) ==
                ConfigEditor.stamp(b, 0, ConfigOrigin.WATCH, 0)
    }
}
