package com.example.personalfinances.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.personalfinances.data.local.db.AppDatabase
import com.example.personalfinances.data.local.db.Migrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * Guards the database against data loss when the schema changes.
 *
 * [MigrationTestHelper] builds a real database at an old version from the schema JSON exported
 * to `app/schemas/`, so a migration can be run against a database shaped exactly like the one on
 * a user's phone. Run with `./gradlew :app:connectedDebugAndroidTest` (needs a device or
 * emulator).
 *
 * When you add a schema version N+1:
 *  1. Write an `AutoMigration(from = N, to = N + 1)` in [AppDatabase], or a hand-written
 *     `Migration(N, N + 1)` in `Migrations.kt`, and register it in `DatabaseModule`.
 *  2. Add a test here: create the database at N with [MigrationTestHelper.createDatabase],
 *     insert sample rows with plain SQL, then call [MigrationTestHelper.runMigrationsAndValidate]
 *     for N + 1 and assert the rows survived. Add the migration to [allMigrations] too.
 *  3. Commit the new `app/schemas/.../N+1.json`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    /** Every migration, in order; the full chain is applied when opening an old database. */
    private val allMigrations = arrayOf(Migrations.MIGRATION_9_10)

    /** The current schema can be created and then opened by the current database class. */
    @Test
    fun latestSchemaOpens() {
        helper.createDatabase(TEST_DB, LATEST).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*allMigrations)
            .build().apply {
                openHelper.writableDatabase.close()
                close()
            }
    }

    /** Dates written as local-midnight milliseconds become the same calendar date as epoch days. */
    @Test
    fun migrate9To10_convertsDatesToEpochDays() {
        val date = LocalDate.of(2026, 3, 15)
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        helper.createDatabase(TEST_DB, 9).apply {
            execSQL("INSERT INTO categories (id, name, transaction_type) VALUES ('c1', 'Groceries', 'EXPENSE')")
            execSQL(
                "INSERT INTO transactions (id, transaction_type, amount, date, cadence_unit, cadence_value, " +
                    "category_id, merchant_id, is_recurring, recurring_group_id, notes, tags) VALUES " +
                    "('t1', 'EXPENSE', 12.5, $millis, 'MONTHS', 0, 'c1', NULL, 0, NULL, NULL, '[]')"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 10, true, Migrations.MIGRATION_9_10)
        migrated.query("SELECT date, amount FROM transactions WHERE id = 't1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(date.toEpochDay(), cursor.getLong(0))
            assertEquals(12.5, cursor.getDouble(1), 0.0)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val LATEST = 10
    }
}
