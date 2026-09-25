package de.j4velin.calendarWidget.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class MonthGridTest {

    private val zone = ZoneId.of("Europe/Berlin")

    @Test
    fun gridStartsOnTheConfiguredWeekDay() {
        // September 2026 starts on a Tuesday
        val today = LocalDate.of(2026, 9, 25)
        val monday = MonthGrid.of(today, 0, startOnMonday = true)
        assertEquals(YearMonth.of(2026, 9), monday.month)
        assertEquals(LocalDate.of(2026, 8, 31), monday.firstDay)
        assertEquals(DayOfWeek.MONDAY, monday.weekDays.first())

        val sunday = MonthGrid.of(today, 0, startOnMonday = false)
        assertEquals(LocalDate.of(2026, 8, 30), sunday.firstDay)
        assertEquals(DayOfWeek.SUNDAY, sunday.weekDays.first())
        assertEquals(42, sunday.days.size)
    }

    @Test
    fun alwaysShowsADayOfThePreviousMonth() {
        // June 2026 starts on a Monday
        val grid = MonthGrid.of(LocalDate.of(2026, 6, 10), 0, startOnMonday = true)
        assertEquals(LocalDate.of(2026, 5, 25), grid.firstDay)
    }

    @Test
    fun appliesMonthOffset() {
        val today = LocalDate.of(2026, 12, 15)
        assertEquals(YearMonth.of(2027, 1), MonthGrid.of(today, 1, true).month)
        assertEquals(YearMonth.of(2026, 10), MonthGrid.of(today, -2, true).month)
    }

    @Test
    fun countsEventsOnEveryDayTheyLast() {
        val grid = MonthGrid.of(LocalDate.of(2026, 9, 25), 0, startOnMonday = true)
        fun at(day: LocalDate, hour: Int) = day.atTime(LocalTime.of(hour, 0)).atZone(zone)
            .toInstant().toEpochMilli()

        val d1 = LocalDate.of(2026, 9, 10)
        val instances = listOf(
            Instance(1, "a", null, false, at(d1, 10), at(d1, 11), 0, false),
            Instance(2, "b", null, false, at(d1, 20), at(d1.plusDays(2), 8), 0, false),
            Instance(3, "declined", null, false, at(d1, 10), at(d1, 11), 0, true),
            // ends at midnight, does not count on the next day
            Instance(4, "c", null, false, at(d1.plusDays(3), 22), d1.plusDays(4).startMillis(zone), 0, false),
            // started before the grid
            Instance(5, "d", null, true, LocalDate.of(2026, 8, 1).startMillis(zone),
                LocalDate.of(2026, 9, 1).startMillis(zone), 0, false),
        )
        val counts = countEventsPerDay(instances, grid, zone)
        assertEquals(2, counts[d1])
        assertEquals(1, counts[d1.plusDays(1)])
        assertEquals(1, counts[d1.plusDays(2)])
        assertEquals(1, counts[d1.plusDays(3)])
        assertEquals(null, counts[d1.plusDays(4)])
        assertEquals(1, counts[LocalDate.of(2026, 8, 31)])
        assertEquals(null, counts[LocalDate.of(2026, 9, 1)])
    }
}
