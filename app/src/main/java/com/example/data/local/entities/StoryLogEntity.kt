package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_logs")
data class StoryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val title: String,
    val summary: String,
    val inGameDate: String = "Año 1042 de la Tercera Era",
    val timestamp: Long = System.currentTimeMillis(),
    val isMilestone: Boolean = false
)
