package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY createdAt DESC")
    fun getAllCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE role = 'NPC' ORDER BY name ASC")
    fun getNpcCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE role = 'USER' ORDER BY name ASC")
    fun getUserCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE role = 'USER' AND isSelectedUserCharacter = 1 LIMIT 1")
    fun getSelectedUserCharacter(): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    fun getCharacterById(id: Long): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterByIdSync(id: Long): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacter(id: Long)

    @Query("UPDATE characters SET isSelectedUserCharacter = 0 WHERE role = 'USER'")
    suspend fun clearSelectedUserCharacter()

    @Query("UPDATE characters SET isSelectedUserCharacter = 1 WHERE id = :id")
    suspend fun markUserCharacterSelected(id: Long)

    @Transaction
    suspend fun selectUserCharacter(id: Long) {
        clearSelectedUserCharacter()
        markUserCharacterSelected(id)
    }
}
