package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String, // "NPC" or "USER"
    val personality: String, // Up to 50,000 chars
    val background: String, // Up to 50,000 chars
    val dialogueExamples: String, // Up to 50,000 chars
    val initialGreeting: String,
    val avatarUri: String? = null,
    val statsJson: String = "{}", // e.g. {"STR": 16, "DEX": 14, "CON": 15, "INT": 12, "WIS": 10, "CHA": 18, "HP": 28, "MAX_HP": 28, "LVL": 3}
    val linkedPlaylistIds: String = "", // Comma-separated IDs, up to 5
    val worldId: Long? = null,
    val isNarratorControlled: Boolean = true,
    val isSelectedUserCharacter: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
