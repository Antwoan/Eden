package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_memories")
data class PinnedMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val content: String,
    val importance: Int = 1, // 1 to 5
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
