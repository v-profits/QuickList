package com.quicklist.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val TYPE_SHOPPING = 0
const val TYPE_TODO = 1

const val REPEAT_NONE = 0
const val REPEAT_DAILY = 1
const val REPEAT_WEEKLY = 2
const val REPEAT_MONTHLY = 3

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,
    val createdAt: Long = System.currentTimeMillis(),
    /** Ordinal of [com.quicklist.app.util.SortMode]. 2 = newest first. */
    val sortMode: Int = 2,
    val groupByCategory: Boolean = true,
    /** Index in ListIconSet; -1 = choose by list type (lists created before icons existed). */
    @ColumnInfo(defaultValue = "-1") val icon: Int = -1
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(
        entity = ListEntity::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listId")]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val note: String = "",
    val quantity: String = "",
    val price: Double? = null,
    /** Ordinal of [Category]. */
    val category: Int = 0,
    /** 0 = normal, 1 = medium, 2 = high. */
    val priority: Int = 0,
    val dueAt: Long? = null,
    val repeat: Int = REPEAT_NONE,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long? = null
)

data class ListCount(val listId: Long, val total: Int, val done: Int)
