package de.j4velin.calendarWidget.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal const val MONTH_GRID_WEEKS = 6

internal data class MonthGrid(
    val month: YearMonth,
    /** the first day shown in the grid, always a day of the previous month */
    val firstDay: LocalDate,
) {
    val days: List<LocalDate> = List(MONTH_GRID_WEEKS * 7) { firstDay.plusDays(it.toLong()) }
    val weekDays: List<DayOfWeek> = List(7) { firstDay.plusDays(it.toLong()).dayOfWeek }
    val lastDay: LocalDate get() = days.last()

    companion object {
        fun of(today: LocalDate, monthOffset: Int, startOnMonday: Boolean): MonthGrid {
            val month = YearMonth.from(today).plusMonths(monthOffset.toLong())
            val first = month.atDay(1)
            val weekStart = if (startOnMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
            var shift = (first.dayOfWeek.value - weekStart.value + 7) % 7
            // always show at least one day of the previous month
            if (shift == 0) shift = 7
            return MonthGrid(month, first.minusDays(shift.toLong()))
        }
    }
}

/**
 * Counts the events per day of the grid. Events lasting several days are counted on each day.
 */
internal fun countEventsPerDay(
    instances: List<Instance>,
    grid: MonthGrid,
    zone: ZoneId,
): Map<LocalDate, Int> {
    val counts = HashMap<LocalDate, Int>()
    for (instance in instances) {
        if (instance.declined) continue
        var day = instance.begin.toLocalDate(zone)
        val last = (if (instance.end > instance.begin) instance.end - 1 else instance.begin)
            .toLocalDate(zone)
        if (day < grid.firstDay) day = grid.firstDay
        while (day <= last && day <= grid.lastDay) {
            counts[day] = (counts[day] ?: 0) + 1
            day = day.plusDays(1)
        }
    }
    return counts
}
