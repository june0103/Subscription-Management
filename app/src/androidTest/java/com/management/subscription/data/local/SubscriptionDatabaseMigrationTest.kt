package com.management.subscription.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 이미 출시된 앱의 v2 DB가 v3으로 올라가도 구독이 그대로 남는지 확인한다.
 * 스토어에 나간 1.0이 DB v2였다. v1 스키마는 내보낸 적이 없어 1→2는 테스트하지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class SubscriptionDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SubscriptionDatabase::class.java
    )

    @Test
    fun migrate2To3_keepsExistingSubscriptionsAndAddsEmptyColumns() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO subscriptions (
                    id, name, amountMinor, currencyCode, serviceKey, linkedPackageName,
                    billingCycle, billingDay, annualMonth, reminderDaysBefore,
                    accentColorRes, createdAt, updatedAt
                ) VALUES (
                    'sub-1', 'Netflix', 17000, 'KRW', 'netflix', 'com.netflix.mediaclient',
                    'MONTHLY', 15, NULL, 3, 0, 1000, 2000
                )
                """.trimIndent()
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, SubscriptionDatabase.MIGRATION_2_3).use { db ->
            db.query(
                "SELECT name, amountMinor, billingDay, reminderDaysBefore, category, paymentMethod, memo " +
                    "FROM subscriptions WHERE id = 'sub-1'"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("Netflix", cursor.getString(0))
                assertEquals(17000L, cursor.getLong(1))
                assertEquals(15, cursor.getInt(2))
                assertEquals(3, cursor.getInt(3))
                assertNull(cursor.getString(4))
                assertNull(cursor.getString(5))
                assertNull(cursor.getString(6))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
