package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_places")
data class WorldPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val worldId: Long,
    val name: String,
    val description: String,
    val coordinateX: Float = 0.5f,
    val coordinateY: Float = 0.5f,
    val isCurrentLocation: Boolean = false
)
