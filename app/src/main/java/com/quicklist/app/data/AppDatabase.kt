package com.quicklist.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY createdAt, id")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY createdAt, id")
    suspend fun getAll(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observe(id: Long): Flow<ListEntity?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun get(id: Long): ListEntity?

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun count(): Int

    @Insert
    suspend fun insert(list: ListEntity): Long

    @Update
    suspend fun update(list: ListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE listId = :listId")
    fun observeForList(listId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE listId = :listId")
    suspend fun getForList(listId: Long): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemEntity?

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM items WHERE listId = :listId AND done = 1")
    suspend fun clearDone(listId: Long)

    @Query("UPDATE items SET done = 0, doneAt = NULL WHERE listId = :listId")
    suspend fun uncheckAll(listId: Long)

    @Query("SELECT listId AS listId, COUNT(*) AS total, SUM(CASE WHEN done = 1 THEN 1 ELSE 0 END) AS done FROM items GROUP BY listId")
    fun observeCounts(): Flow<List<ListCount>>

    @Query("SELECT title FROM items GROUP BY LOWER(title) ORDER BY COUNT(*) DESC, MAX(createdAt) DESC LIMIT 300")
    suspend fun suggestionTitles(): List<String>

    @Query("SELECT * FROM items WHERE done = 0 AND dueAt IS NOT NULL AND dueAt > :now")
    suspend fun upcoming(now: Long): List<ItemEntity>

    @Query("SELECT id FROM items WHERE listId = :listId AND dueAt IS NOT NULL")
    suspend fun idsWithDue(listId: Long): List<Long>
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lists ADD COLUMN icon INTEGER NOT NULL DEFAULT -1")
    }
}

@Database(entities = [ListEntity::class, ItemEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lists(): ListDao
    abstract fun items(): ItemDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "quicklist.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
