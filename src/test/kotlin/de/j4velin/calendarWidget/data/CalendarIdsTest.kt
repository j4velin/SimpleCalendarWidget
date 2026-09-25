package de.j4velin.calendarWidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarIdsTest {

    @Test
    fun parse() {
        assertEquals(emptySet<Long>(), CalendarIds.parse(null))
        assertEquals(emptySet<Long>(), CalendarIds.parse(""))
        assertEquals(setOf(1L, 5L, 12L), CalendarIds.parse("1,5,12"))
        assertEquals(setOf(3L), CalendarIds.parse("3,invalid"))
    }

    @Test
    fun format() {
        assertNull(CalendarIds.format(emptySet()))
        assertEquals(setOf(1L, 2L), CalendarIds.parse(CalendarIds.format(setOf(1L, 2L))))
    }
}
