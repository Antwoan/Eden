package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.WorldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldDao {
    @Query("SELECT * FROM worlds ORDER BY lastPlayedAt DESC, createdAt DESC")
    fun getAllWorlds(): Flow<List<WorldEntity>>

    @Query("SELECT * FROM worlds WHERE isActive = 1 LIMIT 1")
    fun getActiveWorld(): Flow<WorldEntity?>

    @Query("SELECT * FROM worlds WHERE id = :id LIMIT 1")
    fun getWorldById(id: Long): Flow<WorldEntity?>

    @Query("SELECT * FROM worlds WHERE id = :id LIMIT 1")
    suspend fun getWorldByIdSync(id: Long): WorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorld(world: WorldEntity): Long

    @Update
    suspend fun updateWorld(world: WorldEntity)

    @Query("DELETE FROM worlds WHERE id = :id")
    suspend fun deleteWorld(id: Long)

    @Query("UPDATE worlds SET isActive = 0")
    suspend fun clearActiveWorlds()

    @Query("UPDATE worlds SET isActive = 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun markWorldActive(id: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setActiveWorld(id: Long) {
        clearActiveWorlds()
        markWorldActive(id, System.currentTimeMillis())
    }
}
