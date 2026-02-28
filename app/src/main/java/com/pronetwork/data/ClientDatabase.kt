package com.pronetwork.app.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Client::class, Payment::class, PaymentTransaction::class,
        Building::class, SyncQueueEntity::class],
    version = 7,
    // ✅ الطبقة 1: تفعيل تصدير الـ Schema
    // يحفظ نسخة JSON من كل version عشان نقدر نتحقق ونختبر الـ migrations
    exportSchema = true
)
abstract class ClientDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao
    abstract fun buildingDao(): BuildingDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        private const val TAG = "ClientDatabase"

        @Volatile
        private var INSTANCE: ClientDatabase? = null

        fun getDatabase(context: Context): ClientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClientDatabase::class.java,
                    "client_database"
                )
                    // ✅ الطبقة 2: كل الـ migrations معرّفة - لا فجوات
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7

                    )
                    // ✅ الطبقة 3: فقط عند الـ downgrade يحذف ويعيد البناء
                    // عند الـ upgrade بدون migration → crash واضح (لا حذف صامت للبيانات)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    // ✅ الطبقة 4: Callback للتحقق من سلامة البيانات بعد كل فتح
                    .addCallback(DatabaseIntegrityCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * طبقة 4: فحص سلامة قاعدة البيانات عند كل فتح.
         * إذا فيه corruption → نسجل الخطأ (مستقبلاً: نرسل تقرير للسيرفر).
         */
        private class DatabaseIntegrityCallback : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    db.query("PRAGMA quick_check").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val result = cursor.getString(0)
                            if (result != "ok") {
                                Log.e(TAG, "DATABASE INTEGRITY ISSUE: $result")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Integrity check failed: ${e.message}")
                }
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i(TAG, "Database created fresh — version 7")
            }
        }

        // === MIGRATIONS ===

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 → 2: لا تغييرات
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clientId` INTEGER NOT NULL,
                        `month` TEXT NOT NULL,
                        `isPaid` INTEGER NOT NULL DEFAULT 0,
                        `paymentDate` INTEGER,
                        `amount` REAL NOT NULL DEFAULT 0.0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_payments_clientId_month` 
                    ON `payments` (`clientId`, `month`)
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO payments (clientId, month, isPaid, paymentDate, amount, createdAt)
                    SELECT id, startMonth, isPaid, paymentDate, price,
                        COALESCE(paymentDate, strftime('%s','now') * 1000)
                    FROM clients WHERE isPaid = 1
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN firstMonthAmount REAL")
                db.execSQL("ALTER TABLE clients ADD COLUMN startDay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE clients SET firstMonthAmount = price WHERE firstMonthAmount IS NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payment_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `paymentId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`paymentId`) REFERENCES `payments`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_payment_transactions_paymentId`
                    ON `payment_transactions` (`paymentId`)
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` INTEGER NOT NULL,
                        `action` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `lastError` TEXT
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payment_transactions ADD COLUMN createdBy TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
