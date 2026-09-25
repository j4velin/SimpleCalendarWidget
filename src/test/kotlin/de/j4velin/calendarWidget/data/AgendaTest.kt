package de.j4velin.calendarWidget.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class AgendaTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 9, 25)
    private val now = at(today, 12)
    private val lookAheadEnd = today.plusDays(28).startMillis(zone)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(day, java.time.LocalTime.of(hour, minute)).atZone(zone).toInstant()
            .toEpochMilli()

    private fun instance(
        begin: Long, end: Long, title: String = "event", allDay: Boolean = false,
        declined: Boolean = false, id: Long = title.hashCode().toLong(),
    ) = Instance(id, title, null, allDay, begin, end, 0, declined)

    private fun build(
        vararg instances: Instance, everyDay: Boolean = true, skipPassed: Boolean = false,
    ) = buildAgenda(instances.toList(), now, zone, lookAheadEnd, everyDay, skipPassed)

    private fun Day.localDate() = date.toLocalDate(zone)

    @Test
    fun groupsEventsByDay() {
        val agenda = build(
            instance(at(today.plusDays(1), 9), at(today.plusDays(1), 10), "tomorrow"),
            instance(at(today, 14), at(today, 15), "today 2"),
            instance(at(today, 13), at(today, 14), "today 1"),
        )
        assertEquals(listOf(today, today.plusDays(1)), agenda.days.map { it.localDate() })
        assertEquals(listOf("today 1", "today 2"), agenda.days[0].events.map { it.title })
        assertEquals(listOf("tomorrow"), agenda.days[1].events.map { it.title })
    }

    @Test
    fun skipsDeclinedEvents() {
        val agenda = build(instance(at(today, 14), at(today, 15), declined = true))
        assertTrue(agenda.days.isEmpty())
    }

    @Test
    fun skipsEventsAfterLookAheadEnd() {
        val agenda = build(instance(lookAheadEnd + 1, lookAheadEnd + 3_600_000))
        assertTrue(agenda.days.isEmpty())
    }

    @Test
    fun skipsEventsWhichEndedBeforeToday() {
        val yesterday = today.minusDays(1)
        val agenda = build(
            instance(at(yesterday, 22), at(yesterday, 23), "yesterday"),
            instance(at(yesterday, 23), today.startMillis(zone), "until midnight"),
            everyDay = false,
        )
        assertTrue(agenda.days.isEmpty())
    }

    @Test
    fun passedEventsAreShownFirstOrSkipped() {
        val passed = instance(at(today, 8), at(today, 9), "passed")
        val later = instance(at(today, 7), at(today, 18), "running")
        assertEquals(
            listOf("passed", "running"), build(later, passed).days.single().events.map { it.title }
        )
        assertEquals(
            listOf("running"),
            build(later, passed, skipPassed = true).days.single().events.map { it.title },
        )
    }

    @Test
    fun nextChangeIsTheNextEventEnd() {
        val agenda = build(
            instance(at(today, 8), at(today, 9)),
            instance(at(today, 13), at(today, 15), "a"),
            instance(at(today, 13), at(today, 14), "b"),
        )
        assertEquals(at(today, 14), agenda.nextChange)
        assertNull(build().nextChange)
    }

    @Test
    fun multiDayEventOnEveryDay() {
        val agenda = build(instance(at(today, 20), at(today.plusDays(2), 10), "trip"))
        assertEquals(
            listOf(today, today.plusDays(1), today.plusDays(2)), agenda.days.map { it.localDate() }
        )
        val events = agenda.days.map { it.events.single() }
        assertTrue(events.all { it.multiDay })
        assertEquals(listOf(true, false, false), events.map { it.multiDayIsOriginal })
    }

    @Test
    fun multiDayEventOnlyOnFirstDay() {
        val agenda = build(
            instance(at(today, 20), at(today.plusDays(2), 10), "trip"), everyDay = false
        )
        val event = agenda.days.single().events.single()
        assertTrue(event.multiDay)
        assertTrue(event.multiDayIsOriginal)
    }

    @Test
    fun eventEndingAtMidnightIsNotMultiDay() {
        val agenda = build(instance(at(today, 22), today.plusDays(1).startMillis(zone)))
        assertFalse(agenda.days.single().events.single().multiDay)
    }

    @Test
    fun runningEventWhichStartedBeforeTodayIsShownToday() {
        val begin = at(today.minusDays(2), 10)
        val end = at(today.plusDays(1), 10)

        val once = build(instance(begin, end), everyDay = false).days
        assertEquals(today, once.first().localDate())
        assertTrue(once.first().events.single().allDay)

        val everyDay = build(instance(begin, end)).days
        assertEquals(listOf(today, today.plusDays(1)), everyDay.map { it.localDate() })
        assertFalse(everyDay.first().events.single().multiDayIsOriginal)
    }

    @Test
    fun allDayEvent() {
        val agenda = build(
            instance(
                today.startMillis(zone), today.plusDays(1).startMillis(zone), allDay = true
            )
        )
        val event = agenda.days.single().events.single()
        assertTrue(event.allDay)
        assertFalse(event.multiDay)
        assertEquals(today, event.end.toLocalDate(zone))
    }

    @Test
    fun allDayTimesAreConvertedFromUtc() {
        val utcMidnight = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        for (zoneId in listOf("America/Los_Angeles", "Europe/Berlin", "Pacific/Auckland")) {
            val localZone = ZoneId.of(zoneId)
            val local = utcMidnightToLocal(utcMidnight, localZone)
            assertEquals(today.startMillis(localZone), local)
        }
    }
}
