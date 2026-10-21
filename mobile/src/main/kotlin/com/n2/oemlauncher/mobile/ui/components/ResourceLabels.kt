package com.n2.oemlauncher.mobile.ui.components

import com.n2.oemlauncher.mobile.R
import com.n2.oemlauncher.mobile.data.UserNotice
import com.n2.oemlauncher.shared.model.IconShape
import com.n2.oemlauncher.shared.model.IconSize
import com.n2.oemlauncher.shared.model.IconStyle
import com.n2.oemlauncher.shared.model.LayoutMode
import com.n2.oemlauncher.shared.model.SortMode
import com.n2.oemlauncher.shared.model.VisualTier
import com.n2.oemlauncher.shared.protocol.SyncPhase

fun LayoutMode.label(): Int = when (this) {
    LayoutMode.HONEYCOMB -> R.string.honeycomb
    LayoutMode.GRID -> R.string.grid
    LayoutMode.LIST -> R.string.list
}
fun IconSize.label(): Int = when (this) {
    IconSize.SMALL -> R.string.small
    IconSize.MEDIUM -> R.string.medium
    IconSize.LARGE -> R.string.large
}
fun IconShape.label(): Int = when (this) {
    IconShape.CIRCLE -> R.string.circle
    IconShape.SQUIRCLE -> R.string.squircle
    IconShape.ORIGINAL -> R.string.original
}
fun SortMode.label(): Int = when (this) {
    SortMode.CUSTOM -> R.string.custom
    SortMode.ALPHABETICAL -> R.string.alphabetical
    SortMode.MOST_USED -> R.string.most_used
    SortMode.RECENTLY_USED -> R.string.recently_used
}
fun VisualTier.label(): Int = when (this) {
    VisualTier.REFINED -> R.string.tier_refined
    VisualTier.LITE -> R.string.tier_lite
}
fun IconStyle.label(): Int = when (this) {
    IconStyle.ORIGINAL -> R.string.icon_style_original
    IconStyle.THEMED -> R.string.icon_style_themed
}
fun SyncPhase.label(): Int = when (this) {
    SyncPhase.NOT_SENT -> R.string.sync_not_sent
    SyncPhase.WAITING_FOR_WATCH -> R.string.sync_waiting
    SyncPhase.APPLIED -> R.string.sync_applied
    SyncPhase.APPLIED_MISSING_ICONS -> R.string.sync_missing_icons
    SyncPhase.CONFLICT -> R.string.sync_conflict
    SyncPhase.REJECTED -> R.string.sync_rejected
    SyncPhase.WATCH_OUTDATED -> R.string.sync_outdated
}
fun UserNotice.label(): Int = when (this) {
    UserNotice.FAVORITE_LIMIT -> R.string.notice_favorite_limit
    UserNotice.ICON_LIMIT -> R.string.notice_icon_limit
    UserNotice.IMPORT_FAILED -> R.string.notice_import_failed
    UserNotice.ICON_FAILED -> R.string.notice_icon_failed
    UserNotice.SYNC_FAILED -> R.string.notice_sync_failed
    UserNotice.DATA_INVALID -> R.string.notice_data_invalid
    UserNotice.ICONS_MISSING -> R.string.notice_icons_missing
    UserNotice.NONE -> R.string.dismiss
}
