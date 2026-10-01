package com.management.subscription.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SubscriptionEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(BillingCycleConverters::class)
abstract class SubscriptionDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var instance: SubscriptionDatabase? = null

        fun getInstance(context: Context): SubscriptionDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SubscriptionDatabase::class.java,
                    "subscription-database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN serviceKey TEXT")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN linkedPackageName TEXT")
            }
        }

        /** 카테고리·결제수단·메모 추가. 모두 NULL 허용이라 기존 구독은 그대로 남는다. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN paymentMethod TEXT")
                db.execSQL("ALTER TABLE subscriptions ADD COLUMN memo TEXT")
            }
        }
    }
}
