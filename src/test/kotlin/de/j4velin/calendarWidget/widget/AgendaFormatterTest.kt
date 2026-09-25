package de.j4velin.calendarWidget.widget

import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.Day
import de.j4velin.calendarWidget.data.Event
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

class AgendaFormatterTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone(zone))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    private fun at(day: Int, hour: Int) =
        LocalDateTime.of(2026, 9, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun settings(
        showEndTimes: Boolean = false, multiDayOnEveryDay: Boolean = true,
        todayTomorrowText: Boolean = true,
    ) = AgendaSettings(
        timeFormat = "HH:mm", dateFormat = "EEEE, dd.MM.", alldayEndFormat = "dd.MM.",
        showEndTimes = showEndTimes, multiDayOnEveryDay = multiDayOnEveryDay,
        todayTomorrowText = todayTomorrowText,
    )

    private fun formatter(settings: AgendaSettings) =
        AgendaFormatter(settings, "HH:mm", "dd.MM.", "dd.MM.", Locale.ENGLISH)

    private fun event(
        begin: Long, end: Long, allDay: Boolean = false, multiDay: Boolean = false,
        original: Boolean = true,
    ) = Event(1, begin, "title", null, allDay, end, 0, multiDay, original)

    @Test
    fun timedEvent() {
        val e = event(at(25, 10), at(25, 11))
        val day = Day(e.date, listOf(e))
        assertEquals("10:00 ", formatter(settings()).timeText(e, day))
        assertEquals("10:00 - 11:00 ", formatter(settings(showEndTimes = true)).timeText(e, day))
    }

    @Test
    fun allDayEventHasNoTime() {
        val e = event(at(25, 0), at(26, 0) - 1, allDay = true)
        assertNull(formatter(settings()).timeText(e, Day(e.date, listOf(e))))
    }

    @Test
    fun multiDayEvent() {
        val first = event(at(25, 20), at(27, 10), multiDay = true)
        assertEquals("20:00 ", formatter(settings()).timeText(first, Day(first.date, listOf(first))))
        assertEquals(
            "20:00 » 27.09. ",
            formatter(settings(multiDayOnEveryDay = false)).timeText(first, Day(first.date, listOf(first)))
        )
        // continuation on the next days
        val middle = event(at(26, 0), at(27, 10), multiDay = true, original = false)
        assertNull(formatter(settings()).timeText(middle, Day(middle.date, listOf(middle))))
        val last = event(at(27, 0), at(27, 10), multiDay = true, original = false)
        assertEquals("» 10:00 ", formatter(settings()).timeText(last, Day(last.date, listOf(last))))
    }

    @Test
    fun multiDayAllDayEventWithEndDate() {
        val e = event(at(25, 0), at(28, 0) - 1, allDay = true, multiDay = true)
        assertEquals(
            "» 27.09. ",
            formatter(settings(multiDayOnEveryDay = false)).timeText(e, Day(e.date, listOf(e)))
        )
    }

    @Test
    fun dayTitle() {
        val day = Day(at(25, 10), emptyList())
        val formatter = formatter(settings())
        assertEquals("today", formatter.dayTitle(day, 0, "today", "tomorrow"))
        assertEquals("tomorrow", formatter.dayTitle(day, 1, "today", "tomorrow"))
        assertEquals("Friday, 25.09.", formatter.dayTitle(day, 2, "today", "tomorrow"))
        assertEquals(
            "Friday, 25.09.",
            formatter(settings(todayTomorrowText = false)).dayTitle(day, 0, "today", "tomorrow")
        )
    }

    @Test
    fun invalidPatternFallsBack() {
        val formatter = AgendaFormatter(
            settings().copy(timeFormat = "invalid 'quote"), "HH:mm", "dd.MM.", "dd.MM.",
            Locale.ENGLISH,
        )
        val e = event(at(25, 10), at(25, 11))
        assertEquals("10:00 ", formatter.timeText(e, Day(e.date, listOf(e))))
    }
}
