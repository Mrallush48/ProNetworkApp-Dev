package com.pronetwork.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Building::class], version = 2, exportSchema = false)
abstract class BuildingDatabase : RoomDatabase() {
    abstract fun buildingDao(): BuildingDao

    companion object {
        @Volatile
        private var INSTANCE: BuildingDatabase? = null

        fun getDatabase(context: Context): BuildingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuildingDatabase::class.java,
                    "building_database"
                ).addMigrations(Migration1To2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val Migration1To2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration code here if needed
            }
        }
    }
}