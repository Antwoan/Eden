package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.WorldPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldPlaceDao {
    @Query("SELECT * FROM world_places WHERE worldId = :worldId ORDER BY name ASC")
    fun getPlacesForWorld(worldId: Long): Flow<List<WorldPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: WorldPlaceEntity): Long

    @Update
    suspend fun updatePlace(place: WorldPlaceEntity)

    @Query("DELETE FROM world_places WHERE id = :id")
    suspend fun deletePlace(id: Long)

    @Query("UPDATE world_places SET isCurrentLocation = 0 WHERE worldId = :worldId")
    suspend fun clearCurrentLocation(worldId: Long)

    @Query("UPDATE world_places SET isCurrentLocation = 1 WHERE id = :id")
    suspend fun markCurrentLocation(id: Long)
}
