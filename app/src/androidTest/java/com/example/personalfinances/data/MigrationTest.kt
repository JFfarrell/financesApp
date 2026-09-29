package com.example.personalfinances.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinances.data.local.db.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the database against data loss when the schema changes.
 *
 * [MigrationTestHelper] builds a real database at an old version from the schema JSON exported
 * to `app/schemas/`, so a migration can be run against a database shaped exactly like the one on
 * a user's phone. Run with `./gradlew connectedDebugAndroidTest` (needs a device or emulator).
 *
 * When you add a schema version N+1:
 *  1. Write an `AutoMigration(from = N, to = N + 1)` in [AppDatabase], or a hand-written
 *     `Migration(N, N + 1)` passed to the database builder in `DatabaseModule`.
 *  2. Add a test here: create the database at N with [MigrationTestHelper.createDatabase],
 *     insert sample rows with plain SQL, then call [MigrationTestHelper.runMigrationsAndValidate]
 *     for N + 1 and assert the rows survived.
 *  3. Commit the new `app/schemas/.../N+1.json`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    /** The baseline (version 9) schema can be created and opened by the current database class. */
    @Test
    fun baselineSchemaOpens() {
        helper.createDatabase(TEST_DB, 9).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB).build().apply {
            openHelper.writableDatabase.close()
            close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
