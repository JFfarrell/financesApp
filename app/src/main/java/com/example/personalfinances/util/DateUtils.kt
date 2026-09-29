package com.example.personalfinances.util

import java.time.LocalDate
import java.time.YearMonth

object DateUtils {
    /**
     * Returns the inclusive [start, end] date range for [month], where each period begins on
     * [startDay] of the calendar month (default 1 = standard calendar month). The end is the day
     * before the next period starts, matching the repository's inclusive date-range queries.
     *
     * Example with startDay = 25: for May 2026 returns May 25 to Jun 24.
     */
    fun monthDateRange(month: YearMonth, startDay: Int = 1): Pair<LocalDate, LocalDate> {
        val start = month.atDay(startDay.coerceIn(1, month.lengthOfMonth()))
        val next = month.plusMonths(1)
        val nextStart = next.atDay(startDay.coerceIn(1, next.lengthOfMonth()))
        return start to nextStart.minusDays(1)
    }
}
