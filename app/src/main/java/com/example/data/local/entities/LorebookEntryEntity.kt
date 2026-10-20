package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lorebook_entries")
data class LorebookEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val title: String,
    val keywords: String, // Comma separated: e.g. "Imperio, Valyria, Trono"
    val content: String,
    val isEnabled: Boolean = true
)
