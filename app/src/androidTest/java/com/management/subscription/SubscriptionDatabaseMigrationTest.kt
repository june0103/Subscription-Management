package com.management.subscription

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.management.subscription.data.local.SubscriptionDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionDatabaseMigrationTest {

    private val dbName = "subscription-migration-test"

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SubscriptionDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_keepsExistingRowsAndAddsServiceColumns() {
        migrationTestHelper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO subscriptions (
                    id,
                    name,
                    amountMinor,
                    currencyCode,
                    billingCycle,
                    billingDay,
                    annualMonth,
                    reminderDaysBefore,
                    accentColorRes,
                    createdAt,
                    updatedAt
                ) VALUES (
                    'subscription-1',
                    'Netflix',
                    17000,
                    'KRW',
                    'MONTHLY',
                    15,
                    NULL,
                    3,
                    0,
                    1000,
                    1000
                )
                """.trimIndent()
            )
            close()
        }

        val migratedDb = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            2,
            true,
            SubscriptionDatabase.MIGRATION_1_2
        )

        migratedDb.query(
            "SELECT name, serviceKey, linkedPackageName FROM subscriptions WHERE id = 'subscription-1'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Netflix", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }
    }
}
