package com.pronetwork.app.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(
    entities = [Client::class, Payment::class, PaymentTransaction::class, Building::class, SyncQueueEntity::class],
    version = 8,
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
        private const val DB_NAME = "client_database"

        @Volatile
        private var INSTANCE: ClientDatabase? = null

        fun getDatabase(context: Context): ClientDatabase {
            return INSTANCE ?: synchronized(this) {
                // إلزامي: نسخة احتياطية قبل أي migration
                backupDatabase(context)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClientDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(DatabaseIntegrityCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Creates a backup of the database before migration runs.
         * Critical for financial data — allows recovery if migration fails.
         */
        private fun backupDatabase(context: Context) {
            try {
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    val backupDir = File(context.filesDir, "db_backups")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    val backupFile = File(backupDir, "backup_v7_${System.currentTimeMillis()}.db")
                    dbFile.copyTo(backupFile, overwrite = true)
                    Log.i(TAG, "Database backup created: ${backupFile.absolutePath}")

                    // احتفظ بآخر 3 نسخ فقط — حذف القديمة
                    backupDir.listFiles()
                        ?.sortedByDescending { it.lastModified() }
                        ?.drop(3)
                        ?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Database backup failed: ${e.message}")
                // لا نمنع التشغيل — لكن نسجّل الخطأ
            }
        }

        /**
         * فحص سلامة قاعدة البيانات عند كل فتح.
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
                Log.i(TAG, "Database created fresh — version 8")
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
                    COALESCE(paymentDate, strftime('%s','now') * 1000) FROM clients WHERE isPaid = 1
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

        /**
         * MIGRATION_7_8: Convert all IDs from Int to UUIDv7 String
         *
         * ⚠️ CRITICAL: Order matters — parent tables FIRST, then children.
         * buildings → clients → payments → payment_transactions → sync_queue
         *
         * Room wraps this in a transaction automatically.
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Starting MIGRATION_7_8: Int IDs → UUIDv7 String IDs")
                val now = System.currentTimeMillis()

                // ========== 1. BUILDINGS ==========
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
                Log.i(TAG, "MIGRATION_7_8: buildings ✓")

                // ========== 2. CLIENTS ==========
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `clients_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `subscriptionNumber` TEXT NOT NULL,
                        `roomNumber` TEXT,
                        `mobile` TEXT,
                        `price` REAL NOT NULL,
                        `firstMonthAmount` REAL,
                        `buildingId` TEXT NOT NULL,
                        `startMonth` TEXT NOT NULL,
                        `startDay` INTEGER NOT NULL DEFAULT 1,
                        `endMonth` TEXT,
                        `isPaid` INTEGER NOT NULL DEFAULT 0,
                        `paymentDate` INTEGER,
                        `phone` TEXT NOT NULL DEFAULT '',
                        `address` TEXT NOT NULL DEFAULT '',
                        `packageType` TEXT NOT NULL DEFAULT '5Mbps',
                        `notes` TEXT NOT NULL DEFAULT '',
                        `updatedAt` INTEGER NOT NULL DEFAULT $now,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `checksum` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO clients_new (id, name, subscriptionNumber, roomNumber, mobile, price,
                        firstMonthAmount, buildingId, startMonth, startDay, endMonth,
                        isPaid, paymentDate, phone, address, packageType, notes,
                        updatedAt, version, checksum)
                    SELECT CAST(id AS TEXT), name, subscriptionNumber, roomNumber, mobile, price,
                        firstMonthAmount, CAST(buildingId AS TEXT), startMonth, startDay, endMonth,
                        isPaid, paymentDate, COALESCE(phone, ''), COALESCE(address, ''),
                        COALESCE(packageType, '5Mbps'), COALESCE(notes, ''),
                        $now, 1, ''
                    FROM clients
                """.trimIndent())
                db.execSQL("DROP TABLE clients")
                db.execSQL("ALTER TABLE clients_new RENAME TO clients")
                Log.i(TAG, "MIGRATION_7_8: clients ✓")

                // ========== 3. PAYMENTS ==========
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments_new` (
                        `id` TEXT NOT NULL,
                        `clientId` TEXT NOT NULL,
                        `month` TEXT NOT NULL,
                        `isPaid` INTEGER NOT NULL DEFAULT 0,
                        `paymentDate` INTEGER,
                        `amount` REAL NOT NULL DEFAULT 0.0,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `lastModifiedBy` INTEGER,
                        `updatedAt` INTEGER NOT NULL DEFAULT $now,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `checksum` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO payments_new (id, clientId, month, isPaid, paymentDate, amount, notes,
                        lastModifiedBy, updatedAt, version, checksum)
                    SELECT CAST(id AS TEXT), CAST(clientId AS TEXT), month, isPaid, paymentDate, amount,
                        COALESCE(notes, ''), lastModifiedBy,
                        $now, 1, ''
                    FROM payments
                """.trimIndent())
                db.execSQL("DROP TABLE payments")
                db.execSQL("ALTER TABLE payments_new RENAME TO payments")
                Log.i(TAG, "MIGRATION_7_8: payments ✓")

                // ========== 4. PAYMENT_TRANSACTIONS ==========
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payment_transactions_new` (
                        `id` TEXT NOT NULL,
                        `paymentId` TEXT NOT NULL,
                        `type` TEXT NOT NULL DEFAULT '',
                        `amount` REAL NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdBy` INTEGER,
                        `timestamp` INTEGER NOT NULL DEFAULT $now,
                        `updatedAt` INTEGER NOT NULL DEFAULT $now,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO payment_transactions_new (id, paymentId, type, amount, notes, createdBy,
                        timestamp, updatedAt, version)
                    SELECT CAST(id AS TEXT), CAST(paymentId AS TEXT), '', amount,
                        COALESCE(notes, ''), CAST(NULLIF(createdBy, '') AS INTEGER),
                        date, $now, 1
                    FROM payment_transactions
                """.trimIndent())
                db.execSQL("DROP TABLE payment_transactions")
                db.execSQL("ALTER TABLE payment_transactions_new RENAME TO payment_transactions")
                Log.i(TAG, "MIGRATION_7_8: payment_transactions ✓")

                // ========== 5. SYNC_QUEUE ==========
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL DEFAULT '',
                        `payload` TEXT NOT NULL DEFAULT '',
                        `idempotencyKey` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL DEFAULT $now,
                        `retryCount` INTEGER NOT NULL DEFAULT 0,
                        `status` TEXT NOT NULL DEFAULT 'pending'
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO sync_queue_new (id, entityType, entityId, operation, payload, idempotencyKey,
                        createdAt, retryCount, status)
                    SELECT id, entityType, CAST(entityId AS TEXT), action, payload, '',
                        CAST(createdAt AS INTEGER), retryCount, 'pending'
                    FROM sync_queue
                """.trimIndent())
                db.execSQL("DROP TABLE sync_queue")
                db.execSQL("ALTER TABLE sync_queue_new RENAME TO sync_queue")
                Log.i(TAG, "MIGRATION_7_8: sync_queue ✓")

                // ========== 6. RECREATE INDEXES ==========
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payments_clientId_month` ON `payments` (`clientId`, `month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_transactions_paymentId` ON `payment_transactions` (`paymentId`)")
                Log.i(TAG, "MIGRATION_7_8: indexes ✓")

                Log.i(TAG, "MIGRATION_7_8 COMPLETE ✓")
            }
        }
    }
}
