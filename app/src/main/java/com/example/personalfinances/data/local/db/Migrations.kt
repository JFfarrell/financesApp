package com.example.personalfinances.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.ZoneId

/**
 * Hand-written schema migrations. Register each one in `DatabaseModule` with `.addMigrations`,
 * and add a test for it in `MigrationTest`.
 */
object Migrations {

    /**
     * Version 10 stores transaction dates as epoch days instead of epoch milliseconds at local
     * midnight. The table layout is unchanged; only the meaning of the `date` values differs, so
     * each existing value is converted using the phone's current time zone (the one it was
     * written in, unless the phone changed zones in between).
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val zone = ZoneId.systemDefault()

            // Read everything first, then write, so the cursor is closed before any update.
            val converted = mutableListOf<Pair<String, Long>>()
            db.query("SELECT id, date FROM transactions").use { cursor ->
                while (cursor.moveToNext()) {
                    val millis = cursor.getLong(1)
                    val epochDay = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()
                    converted += cursor.getString(0) to epochDay
                }
            }
            converted.forEach { (id, epochDay) ->
                db.execSQL("UPDATE transactions SET date = ? WHERE id = ?", arrayOf<Any>(epochDay, id))
            }
        }
    }
}
