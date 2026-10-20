package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val parentId: Long? = null, // Parent message ID in the tree
    val senderType: String, // "USER", "NARRATOR", "SYSTEM_EVENT", "DICE_ROLL"
    val senderName: String,
    val speakerCharacterId: Long? = null,
    val content: String,
    val branchIndex: Int = 0, // Branch sibling index (0, 1, 2...)
    val lengthPreference: String = "MEDIUM", // "SHORT", "MEDIUM", "LONG"
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val imageUri: String? = null,
    val mediaType: String? = null // "IMAGE", "MAP", or null
)
