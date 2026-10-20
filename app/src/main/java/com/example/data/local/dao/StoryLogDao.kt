package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.StoryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryLogDao {
    @Query("SELECT * FROM story_logs WHERE worldId = :worldId ORDER BY timestamp DESC")
    fun getLogsForWorld(worldId: Long): Flow<List<StoryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StoryLogEntity): Long

    @Update
    suspend fun updateLog(log: StoryLogEntity)

    @Query("DELETE FROM story_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)
}
