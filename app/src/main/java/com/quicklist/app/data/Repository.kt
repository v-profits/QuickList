package com.quicklist.app.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.quicklist.app.R
import com.quicklist.app.reminders.ReminderScheduler
import com.quicklist.app.util.ParsedItem
import com.quicklist.app.util.QuickParser
import com.quicklist.app.widget.ListWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class Repository private constructor(private val context: Context) {
    private val db = AppDatabase.get(context)
    private val listDao = db.lists()
    private val itemDao = db.items()
    private val prefs = Prefs(context)
    private val seedLock = Mutex()

    // ---- Observing ----
    fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()
    fun observeList(id: Long): Flow<ListEntity?> = listDao.observe(id)
    fun observeItems(listId: Long): Flow<List<ItemEntity>> = itemDao.observeForList(listId)
    fun observeCounts(): Flow<List<ListCount>> = itemDao.observeCounts()

    // ---- One-shot reads ----
    suspend fun allLists(): List<ListEntity> = listDao.getAll()
    suspend fun list(id: Long): ListEntity? = listDao.get(id)
    suspend fun item(id: Long): ItemEntity? = itemDao.get(id)
    suspend fun itemsOf(listId: Long): List<ItemEntity> = itemDao.getForList(listId)
    suspend fun suggestionTitles(): List<String> = itemDao.suggestionTitles()

    suspend fun ensureDefaults() = seedLock.withLock {
        if (!prefs.seeded && listDao.count() == 0) {
            val now = System.currentTimeMillis()
            listDao.insert(ListEntity(name = context.getString(R.string.default_shopping), type = TYPE_SHOPPING, createdAt = now))
            listDao.insert(ListEntity(name = context.getString(R.string.default_todo), type = TYPE_TODO, createdAt = now + 1))
            changed()
        }
        prefs.seeded = true
    }

    // ---- Lists ----
    suspend fun createList(name: String, type: Int, icon: Int = -1): Long {
        val id = listDao.insert(ListEntity(name = name.trim(), type = type, icon = icon))
        changed()
        return id
    }

    suspend fun renameList(id: Long, name: String) {
        listDao.get(id)?.let { listDao.update(it.copy(name = name.trim())) }
        changed()
    }

    suspend fun updateList(list: ListEntity) {
        listDao.update(list)
        changed()
    }

    suspend fun deleteList(id: Long) {
        itemDao.idsWithDue(id).forEach { ReminderScheduler.cancel(context, it) }
        listDao.delete(id)
        changed()
    }

    // ---- Items ----
    suspend fun addParsed(listId: Long, text: String): Int = addItems(listId, QuickParser.parseAll(text))

    suspend fun addItems(listId: Long, parsed: List<ParsedItem>): Int {
        val list = listDao.get(listId) ?: return 0
        if (parsed.isEmpty()) return 0
        val existing = itemDao.getForList(listId)
        val now = System.currentTimeMillis()
        parsed.forEachIndexed { i, p ->
            if (p.done) {
                // Item came from a pasted list already marked as bought/done.
                if (existing.none { it.title.equals(p.title, ignoreCase = true) }) {
                    itemDao.insert(
                        ItemEntity(
                            listId = listId, title = p.title, quantity = p.quantity, priority = p.priority,
                            category = if (list.type == TYPE_SHOPPING) Category.detect(p.title).ordinal else 0,
                            done = true, createdAt = now + i, doneAt = now
                        )
                    )
                }
                return@forEachIndexed
            }
            val revive = existing.firstOrNull { it.done && it.title.equals(p.title, ignoreCase = true) }
            if (revive != null) {
                // Item was bought/done before: bring it back instead of creating a duplicate.
                itemDao.update(
                    revive.copy(
                        done = false, doneAt = null, createdAt = now + i,
                        quantity = p.quantity.ifEmpty { revive.quantity }
                    )
                )
            } else {
                itemDao.insert(
                    ItemEntity(
                        listId = listId,
                        title = p.title,
                        quantity = p.quantity,
                        priority = p.priority,
                        category = if (list.type == TYPE_SHOPPING) Category.detect(p.title).ordinal else 0,
                        createdAt = now + i
                    )
                )
            }
        }
        changed()
        return parsed.size
    }

    suspend fun update(item: ItemEntity) {
        itemDao.update(item)
        ReminderScheduler.schedule(context, item)
        changed()
    }

    suspend fun restore(item: ItemEntity) {
        itemDao.insert(item)
        ReminderScheduler.schedule(context, item)
        changed()
    }

    suspend fun delete(id: Long) {
        ReminderScheduler.cancel(context, id)
        itemDao.delete(id)
        changed()
    }

    suspend fun toggle(id: Long) {
        val item = itemDao.get(id) ?: return
        setDone(id, !item.done)
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val item = itemDao.get(id) ?: return
        val due = item.dueAt
        if (done && item.repeat != REPEAT_NONE && due != null) {
            // Repeating task: move to the next occurrence instead of completing.
            val updated = item.copy(dueAt = nextDue(due, item.repeat))
            itemDao.update(updated)
            ReminderScheduler.schedule(context, updated)
        } else {
            val updated = item.copy(done = done, doneAt = if (done) System.currentTimeMillis() else null)
            itemDao.update(updated)
            ReminderScheduler.schedule(context, updated)
        }
        changed()
    }

    suspend fun clearDone(listId: Long) {
        itemDao.clearDone(listId)
        changed()
    }

    suspend fun uncheckAll(listId: Long) {
        itemDao.uncheckAll(listId)
        changed()
    }

    suspend fun rescheduleAll() {
        itemDao.upcoming(System.currentTimeMillis()).forEach { ReminderScheduler.schedule(context, it) }
    }

    private fun nextDue(from: Long, repeat: Int): Long {
        val c = Calendar.getInstance().apply { timeInMillis = from }
        val now = System.currentTimeMillis()
        do {
            when (repeat) {
                REPEAT_DAILY -> c.add(Calendar.DAY_OF_YEAR, 1)
                REPEAT_WEEKLY -> c.add(Calendar.WEEK_OF_YEAR, 1)
                else -> c.add(Calendar.MONTH, 1)
            }
        } while (c.timeInMillis <= now)
        return c.timeInMillis
    }

    // ---- Backup ----
    suspend fun exportJson(): String {
        val root = JSONObject().put("app", "QuickList").put("version", 1)
        val lists = JSONArray()
        for (l in listDao.getAll()) {
            val items = JSONArray()
            for (i in itemDao.getForList(l.id)) {
                items.put(
                    JSONObject()
                        .put("title", i.title)
                        .put("note", i.note)
                        .put("quantity", i.quantity)
                        .put("price", i.price ?: JSONObject.NULL)
                        .put("category", i.category)
                        .put("priority", i.priority)
                        .put("dueAt", i.dueAt ?: JSONObject.NULL)
                        .put("repeat", i.repeat)
                        .put("done", i.done)
                        .put("createdAt", i.createdAt)
                        .put("doneAt", i.doneAt ?: JSONObject.NULL)
                )
            }
            lists.put(
                JSONObject()
                    .put("name", l.name).put("type", l.type)
                    .put("sortMode", l.sortMode).put("group", l.groupByCategory).put("icon", l.icon)
                    .put("items", items)
            )
        }
        return root.put("lists", lists).toString(2)
    }

    /** Adds the lists from a backup file as new lists. Returns the number of imported lists. */
    suspend fun importJson(text: String): Int {
        val arr = JSONObject(text).getJSONArray("lists")
        var count = 0
        for (a in 0 until arr.length()) {
            val o = arr.getJSONObject(a)
            val listId = listDao.insert(
                ListEntity(
                    name = o.optString("name", "List"),
                    type = o.optInt("type", TYPE_SHOPPING),
                    icon = o.optInt("icon", -1),
                    sortMode = o.optInt("sortMode", 2),
                    groupByCategory = o.optBoolean("group", true)
                )
            )
            val items = o.optJSONArray("items")
            if (items != null) {
                val batch = ArrayList<ItemEntity>()
                for (j in 0 until items.length()) {
                    val x = items.getJSONObject(j)
                    val title = x.optString("title").trim()
                    if (title.isEmpty()) continue
                    batch.add(
                        ItemEntity(
                            listId = listId,
                            title = title,
                            note = x.optString("note"),
                            quantity = x.optString("quantity"),
                            price = if (x.isNull("price")) null else x.optDouble("price"),
                            category = x.optInt("category"),
                            priority = x.optInt("priority"),
                            dueAt = if (x.isNull("dueAt")) null else x.optLong("dueAt"),
                            repeat = x.optInt("repeat"),
                            done = x.optBoolean("done"),
                            createdAt = x.optLong("createdAt", System.currentTimeMillis()),
                            doneAt = if (x.isNull("doneAt")) null else x.optLong("doneAt")
                        )
                    )
                }
                itemDao.insertAll(batch)
            }
            count++
        }
        rescheduleAll()
        changed()
        return count
    }

    private suspend fun changed() {
        try {
            ListWidget().updateAll(context)
        } catch (_: Exception) {
        }
    }

    companion object {
        @Volatile private var instance: Repository? = null

        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}
