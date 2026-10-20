package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.LorebookEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LorebookDao {
    @Query("SELECT * FROM lorebook_entries WHERE worldId = :worldId ORDER BY title ASC")
    fun getLorebookForWorld(worldId: Long): Flow<List<LorebookEntryEntity>>

    @Query("SELECT * FROM lorebook_entries WHERE worldId = :worldId AND isEnabled = 1")
    suspend fun getEnabledEntriesForWorld(worldId: Long): List<LorebookEntryEntity>

    @Query("SELECT * FROM lorebook_entries ORDER BY title ASC")
    fun getAllLorebookEntries(): Flow<List<LorebookEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLorebookEntry(entry: LorebookEntryEntity): Long

    @Update
    suspend fun updateLorebookEntry(entry: LorebookEntryEntity)

    @Query("DELETE FROM lorebook_entries WHERE id = :id")
    suspend fun deleteLorebookEntry(id: Long)
}
