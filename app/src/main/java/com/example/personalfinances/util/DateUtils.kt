package com.example.personalfinances.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

object DateUtils {
    /**
     * Returns the [start, end) epoch-millis window for [month], where each period begins on
     * [startDay] of the calendar month (default 1 = standard calendar month).
     *
     * Example with startDay = 25: for May 2026 returns May 25 00:00 → Jun 25 00:00.
     */
    fun monthBounds(month: YearMonth, startDay: Int = 1): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val day = startDay.coerceIn(1, month.lengthOfMonth())
        val start = month.atDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextDay = startDay.coerceIn(1, month.plusMonths(1).lengthOfMonth())
        val end = month.plusMonths(1).atDay(nextDay).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun Long.toYearMonth(): YearMonth {
        val localDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        return YearMonth.of(localDate.year, localDate.month)
    }

    fun Long.toLocalDate() =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    fun todayEpochMillis(): Long =
        java.time.LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * Converts local-midnight epoch millis (as stored in the DB) to UTC-midnight epoch millis for
     * the same calendar date. Use this when initialising a Material3 [DatePickerState].
     */
    fun Long.toUtcMidnight(): Long {
        val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    /**
     * Converts UTC-midnight epoch millis (as returned by Material3 DatePicker) to local-midnight
     * epoch millis for the same calendar date. Use this when saving a date picked by the user.
     */
    fun Long.fromUtcMidnight(): Long {
        val date = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Returns epoch millis for the date [months] ahead of [fromMillis], preserving the day-of-month. */
    fun addMonths(fromMillis: Long, months: Int): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(fromMillis)
            .atZone(zone)
            .toLocalDate()
            .plusMonths(months.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}
