package com.pronetwork.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Client::class], version = 2, exportSchema = false)
abstract class ClientDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: ClientDatabase? = null

        fun getDatabase(context: Context): ClientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClientDatabase::class.java,
                    "client_database"
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