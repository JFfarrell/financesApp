package com.example.personalfinances.util

import com.example.personalfinances.domain.model.enums.CadenceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Plain JVM tests for repeat dates; run with `./gradlew :app:testDebugUnitTest`. */
class RecurrenceTest {

    @Test
    fun aMonthlyPaymentOnThe1stStaysOnThe1st() {
        val dates = Recurrence.dates(LocalDate.of(2026, 1, 1), CadenceUnit.MONTHS, 1, 14)
        assertEquals(14, dates.size)
        assertTrue(dates.all { it.dayOfMonth == 1 })
        assertEquals(LocalDate.of(2026, 2, 1), dates[1])
        assertEquals(LocalDate.of(2027, 2, 1), dates[13])
    }

    @Test
    fun theFirstOccurrenceIsTheStartDate() {
        assertEquals(listOf(LocalDate.of(2026, 5, 17)), Recurrence.dates(LocalDate.of(2026, 5, 17), CadenceUnit.MONTHS, 1, 1))
    }

    @Test
    fun aStartOnThe31stFallsOnTheLastDayOfShortMonthsThenReturnsTo31() {
        val dates = Recurrence.dates(LocalDate.of(2026, 1, 31), CadenceUnit.MONTHS, 1, 4)
        assertEquals(
            listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30)),
            dates
        )
    }

    @Test
    fun everyNMonthsSkipsMonthsAndKeepsTheDay() {
        val dates = Recurrence.dates(LocalDate.of(2026, 3, 15), CadenceUnit.MONTHS, 3, 3)
        assertEquals(listOf(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 9, 15)), dates)
    }

    @Test
    fun otherUnitsStepByTheirOwnLength() {
        val start = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2026, 1, 15), Recurrence.dates(start, CadenceUnit.DAYS, 7, 3)[2])
        assertEquals(LocalDate.of(2026, 1, 15), Recurrence.dates(start, CadenceUnit.WEEKS, 1, 3)[2])
        assertEquals(LocalDate.of(2028, 1, 1), Recurrence.dates(start, CadenceUnit.YEARS, 1, 3)[2])
    }

    @Test
    fun aMissingOrNonPositiveIntervalOrCountIsSafe() {
        assertTrue(Recurrence.dates(LocalDate.of(2026, 1, 1), CadenceUnit.MONTHS, 1, 0).isEmpty())
        // An interval of 0 is treated as 1 rather than repeating on the same day.
        assertEquals(LocalDate.of(2026, 2, 1), Recurrence.dates(LocalDate.of(2026, 1, 1), CadenceUnit.MONTHS, 0, 2)[1])
    }
}
