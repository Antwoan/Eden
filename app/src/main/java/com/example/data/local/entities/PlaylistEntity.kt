package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ambientType: String, // "Combate Épico", "Sigilo & Infiltración", "Persecución & Fuga", "Investigación & Misterio", "Terror Cósmico", "Romance & Intimidad", "Melancolía & Despedida", "Descanso en Campamento", "Odisea Espacial", "Ciudad Cyberpunk"
    val imageUri: String? = null,
    val tracksJson: String = "[]", // JSON array of up to 20 tracks: [{"id": "...", "title": "...", "uri": "..."}]
    val createdAt: Long = System.currentTimeMillis()
)
