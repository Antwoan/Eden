package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.PinnedMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedMemoryDao {
    @Query("SELECT * FROM pinned_memories WHERE worldId = :worldId ORDER BY importance DESC, createdAt DESC")
    fun getPinnedMemories(worldId: Long): Flow<List<PinnedMemoryEntity>>

    @Query("SELECT * FROM pinned_memories WHERE worldId = :worldId AND isActive = 1 ORDER BY importance DESC")
    suspend fun getActiveMemoriesSync(worldId: Long): List<PinnedMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: PinnedMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: PinnedMemoryEntity)

    @Query("DELETE FROM pinned_memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)
}
