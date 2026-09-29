package com.example.personalfinances.data.local.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Room type converters registered on [AppDatabase].
 *
 * Kept in their own concrete class because Room instantiates the converter class in the generated
 * DAO code, which is impossible for the abstract [AppDatabase]. Stores [LocalDate] as epoch
 * millis at the start of the day in the device's time zone.
 */
class Converters {

    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? =
        value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? =
        date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
}
