package com.example.personalfinances.data.local.db

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Room type converters registered on [AppDatabase].
 *
 * Kept in their own concrete class because Room instantiates the converter class in the generated
 * DAO code, which is impossible for the abstract [AppDatabase].
 *
 * A [LocalDate] is stored as its epoch day: the number of days since 1970-01-01. This is the
 * calendar date itself with no time zone in it, so a stored date reads back identically wherever
 * the phone is and whatever its time zone or daylight saving rules are. (Versions before 10
 * stored midnight in the phone's time zone as milliseconds, which could shift a date by a day
 * after a time zone change; [Migrations.MIGRATION_9_10] converted those.)
 */
class Converters {

    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? = date?.toEpochDay()
}
