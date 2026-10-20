package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE worldId = :worldId ORDER BY timestamp ASC")
    fun getMessagesForWorld(worldId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE worldId = :worldId ORDER BY timestamp ASC")
    suspend fun getMessagesForWorldSync(worldId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE parentId = :parentId ORDER BY branchIndex ASC")
    fun getSiblings(parentId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE parentId = :parentId ORDER BY branchIndex ASC")
    suspend fun getSiblingsSync(parentId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages WHERE worldId = :worldId")
    fun countMessagesForWorld(worldId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE worldId = :worldId")
    suspend fun clearMessagesForWorld(worldId: Long)

    @Query("SELECT * FROM messages WHERE worldId = :worldId AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(worldId: Long, query: String): Flow<List<MessageEntity>>
}
