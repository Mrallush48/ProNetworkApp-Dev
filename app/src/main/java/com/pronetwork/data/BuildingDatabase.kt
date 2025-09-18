package com.pronetwork.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Building::class], version = 1, exportSchema = true)
abstract class BuildingDatabase : RoomDatabase() {
    abstract fun buildingDao(): BuildingDao

    companion object {
        @Volatile
        private var INSTANCE: BuildingDatabase? = null

        fun getDatabase(context: Context): BuildingDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    BuildingDatabase::class.java,
                    "buildings_db"
                )
                    .fallbackToDestructiveMigration() // مبسط للحصول على build نظيف - لو تريد migrations أخبرني
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
