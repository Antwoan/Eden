package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CharacterDao
import com.example.data.local.dao.LorebookDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.PinnedMemoryDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.StoryLogDao
import com.example.data.local.dao.WorldDao
import com.example.data.local.dao.WorldPlaceDao
import com.example.data.local.entities.CharacterEntity
import com.example.data.local.entities.LorebookEntryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.PinnedMemoryEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.StoryLogEntity
import com.example.data.local.entities.WorldEntity
import com.example.data.local.entities.WorldPlaceEntity

@Database(
    entities = [
        WorldEntity::class,
        CharacterEntity::class,
        LorebookEntryEntity::class,
        MessageEntity::class,
        StoryLogEntity::class,
        PinnedMemoryEntity::class,
        PlaylistEntity::class,
        WorldPlaceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun worldDao(): WorldDao
    abstract fun characterDao(): CharacterDao
    abstract fun lorebookDao(): LorebookDao
    abstract fun messageDao(): MessageDao
    abstract fun storyLogDao(): StoryLogDao
    abstract fun pinnedMemoryDao(): PinnedMemoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun worldPlaceDao(): WorldPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mythos_roleplay.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
