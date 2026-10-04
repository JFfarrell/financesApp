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

    /** The last start day (day of the month) whose payday names its own month; later ones name the next. */
    const val OWN_MONTH_CUTOFF_DAY = 15

    /**
     * The first day of the cycle that comes AFTER the one named [month].
     *
     * A cycle is named for the month its payday leads into. A payday on the 1st to the
     * [OWN_MONTH_CUTOFF_DAY]th is early in its month, so it leads into that same month: the cycle
     * named for [month] starts on [startDay] of [month] and the next one on [startDay] of the
     * following month. A later payday (say the 25th) is near the end of its month, so it leads
     * into the next one: the cycle named for [month] starts on [startDay] of the month before
     * and the next one on [startDay] of [month] itself.
     *
     * The month is always the nominal one; the weekend shift moves only the date, never the name.
     */
    private fun nextCycleStart(month: YearMonth, startDay: Int): LocalDate =
        if (startDay <= OWN_MONTH_CUTOFF_DAY) adjustedStartDate(month.plusMonths(1), startDay)
        else adjustedStartDate(month, startDay)

    /**
     * Returns the inclusive [start, end] date range for [month].
     *
     * The cycle is named for the month its payday leads into: pay on 25 Aug funds September, and
     * pay on 1 Feb or 2 Feb funds February (a payday up to [OWN_MONTH_CUTOFF_DAY] names its own
     * month, a later one the next).
     *
     * Example with startDay = 25:
     * September = Aug 25 -> Sep 24
     * October   = Sep 25 -> Oct 24
     *
     * With startDay = 1 a cycle is a calendar month: February = Feb 1 -> Feb 28.
     * With startDay = 2, February = Feb 2 -> Mar 1.
     *
     * If the start day falls on a weekend, it moves backwards to Friday.
     *
     * Example:
     * If Aug 25 is Saturday:
     * September = Aug 24 (Friday) -> Sep 24
     *
     * The shift can reach into the previous month. 1 Feb 2026 is a Sunday, so with startDay = 1
     * the February cycle begins on Friday 30 Jan and January's ends on 29 Jan. Likewise 2 Feb
     * 2025 is a Sunday, so with startDay = 2 February begins on Friday 31 Jan.
     */
    fun monthDateRange(
        month: YearMonth,
        startDay: Int = 1
    ): Pair<LocalDate, LocalDate> {

        val start = nextCycleStart(month.minusMonths(1), startDay)
        val end = nextCycleStart(month, startDay).minusDays(1)

        return start to end
    }

    /**
     * Returns the pay-cycle month that [date] belongs to.
     *
     * The cycle is named for the month its payday leads into: pay on 25 Aug funds September, and
     * pay on 1 Feb funds February.
     *
     * Example with startDay = 25:
     *
     * Aug 24 -> August
     * Aug 25 -> September
     * Sep 24 -> September
     * Sep 25 -> October
     *
     * If the configured start day falls on a weekend, the start
     * is moved backwards to the preceding Friday, which can put the last days of one month into
     * the next month's cycle (see [monthDateRange]).
     *
     * It is the inverse of [monthDateRange]: the answer is the first cycle whose next one has not
     * yet started on [date]. The search starts one month early because a weekend shift can pull a
     * cycle's start a couple of days back, and takes at most a few steps.
     */
    fun cycleMonthOf(
        date: LocalDate,
        startDay: Int = 1
    ): YearMonth {

        var month = YearMonth.from(date).minusMonths(1)
        while (date >= nextCycleStart(month, startDay)) {
            month = month.plusMonths(1)
        }
        return month
    }
}
