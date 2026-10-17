package com.quicklist.app.util

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.quicklist.app.R
import com.quicklist.app.data.ItemEntity
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.TYPE_SHOPPING

enum class SortMode(@StringRes val label: Int) {
    ADDED(R.string.sort_added),
    ALPHA(R.string.sort_az),
    NEWEST(R.string.sort_newest),
    DUE(R.string.sort_due),
    PRIORITY(R.string.sort_priority);

    companion object {
        fun forType(type: Int): List<SortMode> =
            if (type == TYPE_SHOPPING) listOf(NEWEST, ADDED, ALPHA) else listOf(NEWEST, ADDED, ALPHA, DUE, PRIORITY)

        fun fromOrdinal(i: Int): SortMode = entries.getOrElse(i) { NEWEST }
    }
}

fun sortItems(items: List<ItemEntity>, mode: SortMode): List<ItemEntity> = when (mode) {
    SortMode.ADDED -> items.sortedBy { it.createdAt }
    SortMode.ALPHA -> items.sortedBy { it.title.lowercase() }
    SortMode.NEWEST -> items.sortedByDescending { it.createdAt }
    SortMode.DUE -> items.sortedWith(compareBy<ItemEntity> { it.dueAt == null }.thenBy { it.dueAt ?: 0L })
    SortMode.PRIORITY -> items.sortedWith(compareByDescending<ItemEntity> { it.priority }.thenBy { it.createdAt })
}

fun formatDue(context: Context, millis: Long): String {
    val time = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)
    return if (DateUtils.isToday(millis)) {
        "${context.getString(R.string.today)}, $time"
    } else {
        DateUtils.formatDateTime(
            context, millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
        )
    }
}

fun formatPlain(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()

fun buildShareText(list: ListEntity, items: List<ItemEntity>): String = buildString {
    appendLine(list.name)
    items.sortedWith(compareBy<ItemEntity> { it.done }.thenBy { it.createdAt }).forEach {
        append(if (it.done) "✓ " else "☐ ")
        append(it.title)
        if (it.quantity.isNotBlank()) append(" — ").append(it.quantity)
        appendLine()
    }
}
