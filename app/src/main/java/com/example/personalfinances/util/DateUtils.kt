package com.example.personalfinances.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

object DateUtils {
    fun monthBounds(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
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

    /** Returns epoch millis for the first day of the month that is [months] ahead of [fromMillis]. */
    fun addMonths(fromMillis: Long, months: Int): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(fromMillis)
            .atZone(zone)
            .toLocalDate()
            .withDayOfMonth(1)
            .plusMonths(months.toLong())
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}
