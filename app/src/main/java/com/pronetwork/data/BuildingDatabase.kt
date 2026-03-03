package com.pronetwork.app.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Building::class], version = 3, exportSchema = false)
abstract class BuildingDatabase : RoomDatabase() {

    abstract fun buildingDao(): BuildingDao

    companion object {
        private const val TAG = "BuildingDatabase"

        @Volatile
        private var INSTANCE: BuildingDatabase? = null

        fun getDatabase(context: Context): BuildingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuildingDatabase::class.java,
                    "building_database"
                )
                    .addMigrations(Migration1To2, Migration2To3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration code here if needed (currently empty)
            }
        }

        /**
         * Migration 2→3: Convert building IDs from Int to UUIDv7 String
         * + Add updatedAt, version fields for optimistic locking
         */
        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Starting Migration2To3: Int IDs → String IDs")
                val now = System.currentTimeMillis()

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `buildings_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `location` TEXT NOT NULL DEFAULT '',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `floors` INTEGER NOT NULL DEFAULT 0,
                        `managerName` TEXT NOT NULL DEFAULT '',
                        `updatedAt` INTEGER NOT NULL DEFAULT $now,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO buildings_new (id, name, location, notes, floors, managerName, updatedAt, version)
                    SELECT CAST(id AS TEXT), name,
                        COALESCE(location, ''), COALESCE(notes, ''), COALESCE(floors, 0), COALESCE(managerName, ''),
                        $now, 1
                    FROM buildings
                """.trimIndent())

                db.execSQL("DROP TABLE buildings")
                db.execSQL("ALTER TABLE buildings_new RENAME TO buildings")

                Log.i(TAG, "Migration2To3 COMPLETE ✓")
            }
        }
    }
}
