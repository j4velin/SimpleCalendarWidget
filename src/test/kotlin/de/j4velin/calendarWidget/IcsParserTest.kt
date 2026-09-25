package de.j4velin.calendarWidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class IcsParserTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun parse(ics: String) = IcsParser.parse(ics.trimIndent().lineSequence(), zone)

    @Test
    fun parsesTimedEvent() {
        val event = parse(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Team Meeting\, weekly
            DESCRIPTION:Line one\nLine two
            LOCATION;LANGUAGE=en:Room 1
            DTSTART;TZID=America/New_York:20260925T090000
            DTEND:20260925T140000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Second
            END:VEVENT
            END:VCALENDAR
            """
        )!!
        assertEquals("Team Meeting, weekly", event.title)
        assertEquals("Line one\nLine two", event.description)
        assertEquals("Room 1", event.location)
        assertFalse(event.allDay)
        assertEquals(
            ZonedDateTime.of(2026, 9, 25, 9, 0, 0, 0, ZoneId.of("America/New_York")).toInstant()
                .toEpochMilli(), event.begin
        )
        assertEquals(
            ZonedDateTime.of(2026, 9, 25, 14, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli(),
            event.end
        )
    }

    @Test
    fun keepsCaseAndUnfoldsLines() {
        val event = parse(
            """
            begin:vevent
            summary:Birthday party at
              Anna's place
            dtstart:20260925T180000
            end:vevent
            """
        )!!
        assertEquals("Birthday party at Anna's place", event.title)
        assertEquals(
            ZonedDateTime.of(2026, 9, 25, 18, 0, 0, 0, zone).toInstant().toEpochMilli(), event.begin
        )
        assertNull(event.end)
    }

    @Test
    fun parsesAllDayEvent() {
        val event = parse(
            """
            BEGIN:VEVENT
            SUMMARY:Holiday
            DTSTART;VALUE=DATE:20261003
            DTEND;VALUE=DATE:20261004
            END:VEVENT
            """
        )!!
        assertTrue(event.allDay)
        assertEquals(
            LocalDate.of(2026, 10, 3).atStartOfDay(zone).toInstant().toEpochMilli(), event.begin
        )
    }

    @Test
    fun returnsNullWithoutEvent() {
        assertNull(parse("BEGIN:VCALENDAR\nEND:VCALENDAR"))
    }
}
