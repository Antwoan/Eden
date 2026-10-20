package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

object StoryGenres {
    const val DARK_FANTASY = "DARK_FANTASY"
    const val SCI_FI = "SCI_FI"
    const val MODERN_NOIR = "MODERN_NOIR"
    const val CYBERPUNK = "CYBERPUNK"
    const val POST_APOCALYPTIC = "POST_APOCALYPTIC"
    const val COSMIC_HORROR = "COSMIC_HORROR"
    const val CUSTOM = "CUSTOM"
}

@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val lore: String,
    val systemPrompt: String = "",
    val genre: String = StoryGenres.DARK_FANTASY,
    val coverImageUri: String? = null,
    val chatBackgroundUri: String? = null,
    val defaultAmbientType: String = "Descanso en Campamento",
    val isActive: Boolean = false,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
