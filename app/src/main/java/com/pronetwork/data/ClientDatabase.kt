package com.pronetwork.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Client::class, Payment::class],
    version = 4, // رفعنا من 3 إلى 4 لإضافة الحقول الجديدة
    exportSchema = false
)
abstract class ClientDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: ClientDatabase? = null

        fun getDatabase(context: Context): ClientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ClientDatabase::class.java,
                    "client_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4  // Migration الجديد
                    )
                    .fallbackToDestructiveMigration() // للتطوير فقط
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration 1 → 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration code for version 1 to 2
            }
        }

        // Migration 2 → 3: إضافة جدول payments
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // إنشاء جدول payments
                db.execSQL(
                    """
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
                    """.trimIndent()
                )

                // إنشاء index فريد
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_payments_clientId_month` 
                    ON `payments` (`clientId`, `month`)
                    """.trimIndent()
                )

                // نقل البيانات القديمة
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO payments (clientId, month, isPaid, paymentDate, amount, createdAt)
                    SELECT 
                        id,
                        startMonth,
                        isPaid,
                        paymentDate,
                        price,
                        COALESCE(paymentDate, ${System.currentTimeMillis()})
                    FROM clients
                    WHERE isPaid = 1
                    """.trimIndent()
                )
            }
        }

        // Migration 3 → 4: إضافة firstMonthAmount و startDay
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // إضافة عمود firstMonthAmount (المبلغ الجزئي للشهر الأول)
                db.execSQL(
                    """
                    ALTER TABLE clients 
                    ADD COLUMN firstMonthAmount REAL
                    """.trimIndent()
                )

                // إضافة عمود startDay (يوم البداية في الشهر)
                db.execSQL(
                    """
                    ALTER TABLE clients 
                    ADD COLUMN startDay INTEGER NOT NULL DEFAULT 1
                    """.trimIndent()
                )

                // تحديث القيم الافتراضية للعملاء الحاليين
                // firstMonthAmount = price (كامل) للعملاء القدامى
                db.execSQL(
                    """
                    UPDATE clients 
                    SET firstMonthAmount = price 
                    WHERE firstMonthAmount IS NULL
                    """.trimIndent()
                )
            }
        }
    }
}
