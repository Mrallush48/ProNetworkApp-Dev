package com.pronetwork.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Client::class, Payment::class, PaymentTransaction::class],
    version = 5, // رفعنا من 4 إلى 5 لإضافة جدول payment_transactions
    exportSchema = false
)
abstract class ClientDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun paymentDao(): PaymentDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao

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
                        MIGRATION_3_4,
                        MIGRATION_4_5
                    )
                    .fallbackToDestructiveMigration() // للتطوير فقط، يمكن إزالته لاحقاً في الإنتاج
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration 1 → 2 (قديم - كما هو)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration code for version 1 to 2 (لا يوجد شيء حالياً)
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

                // نقل البيانات القديمة من clients إلى payments للسجلات المدفوعة
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

        // Migration 3 → 4: إضافة firstMonthAmount و startDay في clients
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

                // firstMonthAmount = price للعملاء القدامى
                db.execSQL(
                    """
                    UPDATE clients 
                    SET firstMonthAmount = price 
                    WHERE firstMonthAmount IS NULL
                    """.trimIndent()
                )
            }
        }

        // Migration 4 → 5: إنشاء جدول payment_transactions
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payment_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `paymentId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(`paymentId`) REFERENCES `payments`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_payment_transactions_paymentId`
                    ON `payment_transactions` (`paymentId`)
                    """.trimIndent()
                )
            }
        }
    }
}
