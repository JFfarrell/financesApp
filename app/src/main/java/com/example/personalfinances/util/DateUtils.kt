package com.example.personalfinances.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object DateUtils {

    private fun adjustedStartDate(
        month: YearMonth,
        startDay: Int
    ): LocalDate {
        val date = month.atDay(
            startDay.coerceIn(1, month.lengthOfMonth())
        )

        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.minusDays(1)
            DayOfWeek.SUNDAY -> date.minusDays(2)
            else -> date
        }
    }

    /**
     * Returns the inclusive [start, end] date range for [month].
     *
     * The cycle is named after the month in which it ENDS.
     *
     * Example with startDay = 25:
     * September = Aug 25 -> Sep 24
     * October   = Sep 25 -> Oct 24
     *
     * If the start day falls on a weekend, it moves backwards to Friday.
     *
     * Example:
     * If Aug 25 is Saturday:
     * September = Aug 24 (Friday) -> Sep 24
     */
    fun monthDateRange(
        month: YearMonth,
        startDay: Int = 1
    ): Pair<LocalDate, LocalDate> {

        val end = adjustedStartDate(month, startDay).minusDays(1)
        val previousMonth = month.minusMonths(1)
        val start = adjustedStartDate(previousMonth, startDay)

        return start to end
    }

    /**
     * Returns the pay-cycle month that [date] belongs to.
     *
     * The cycle is named after the month in which it ENDS.
     *
     * Example with startDay = 25:
     *
     * Aug 24 -> August
     * Aug 25 -> September
     * Sep 24 -> September
     * Sep 25 -> October
     *
     * If the configured start day falls on a weekend, the start
     * is moved backwards to the preceding Friday.
     */
    fun cycleMonthOf(
        date: LocalDate,
        startDay: Int = 1
    ): YearMonth {

        val month = YearMonth.from(date)
        val start = adjustedStartDate(month, startDay)
        return if (date >= start) {
            month.plusMonths(1)
        } else {
            month
        }
    }
}
