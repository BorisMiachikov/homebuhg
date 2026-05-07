package ru.homebuhg.core.common

import ru.homebuhg.core.data.database.entity.BudgetPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun BudgetPeriod.currentRange(): Pair<Long, Long> {
    val today = LocalDate.now()
    return when (this) {
        BudgetPeriod.WEEK -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            start.startOfDayMillis() to end.endOfDayMillis()
        }
        BudgetPeriod.MONTH -> {
            val start = today.withDayOfMonth(1)
            val end = today.with(TemporalAdjusters.lastDayOfMonth())
            start.startOfDayMillis() to end.endOfDayMillis()
        }
        BudgetPeriod.YEAR -> {
            val start = today.withDayOfYear(1)
            val end = today.with(TemporalAdjusters.lastDayOfYear())
            start.startOfDayMillis() to end.endOfDayMillis()
        }
    }
}
