package com.example.personalfinances.util

import com.example.personalfinances.domain.model.enums.CadenceUnit
import java.time.LocalDate

/** Works out the dates of a repeating transaction. */
object Recurrence {
    /**
     * The dates of [count] occurrences of something that repeats every [value] [unit]s, the first
     * on [start].
     *
     * Every date is worked out from [start], not from the previous occurrence, so the original
     * day is kept: a gym membership first paid on the 1st is on the 1st every month. A start on
     * the 31st falls on the last day of shorter months but returns to the 31st afterwards, rather
     * than drifting to the 28th for good.
     */
    fun dates(start: LocalDate, unit: CadenceUnit, value: Int, count: Int): List<LocalDate> =
        List(count.coerceAtLeast(0)) { i ->
            val steps = value.coerceAtLeast(1).toLong() * i
            when (unit) {
                CadenceUnit.DAYS -> start.plusDays(steps)
                CadenceUnit.WEEKS -> start.plusWeeks(steps)
                CadenceUnit.MONTHS -> start.plusMonths(steps)
                CadenceUnit.YEARS -> start.plusYears(steps)
            }
        }
}
