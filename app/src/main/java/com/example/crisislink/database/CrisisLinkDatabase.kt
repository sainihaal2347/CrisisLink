package com.example.crisislink.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Message::class],
    version = 1,
    exportSchema = false
)
abstract class CrisisLinkDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: CrisisLinkDatabase? = null

        fun getDatabase(context: Context): CrisisLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CrisisLinkDatabase::class.java,
                    "crisislink_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

