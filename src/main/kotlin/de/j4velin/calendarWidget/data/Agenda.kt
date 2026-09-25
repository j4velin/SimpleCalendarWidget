package de.j4velin.calendarWidget.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

internal const val DAY_MS = 24 * 60 * 60 * 1000L

/** One instance of a (possibly recurring) calendar event, as read from the calendar provider */
internal data class Instance(
    val eventId: Long,
    val title: String,
    val location: String?,
    val allDay: Boolean,
    /** start, for all-day events already converted to local midnight */
    val begin: Long,
    /** end, for all-day events already converted to local midnight */
    val end: Long,
    val color: Int,
    val declined: Boolean,
)

internal data class Event(
    val id: Long,
    /** the start time, or the start of the day for the continuation of a multi-day event */
    val date: Long,
    val title: String,
    val location: String?,
    val allDay: Boolean,
    /** end time, for all-day events the last millisecond of the last day */
    val end: Long,
    val color: Int,
    val multiDay: Boolean,
    /** false for the copies of a multi-day event which are shown on the following days */
    val multiDayIsOriginal: Boolean,
) {
    fun isPassed(now: Long) = end in 1 until now
}

internal data class Day(val date: Long, val events: List<Event>)

internal data class Agenda(
    val days: List<Day>,
    /** the next point in time at which the agenda changes because an event ends */
    val nextChange: Long?,
)

internal fun Long.toLocalDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

internal fun LocalDate.startMillis(zone: ZoneId): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * The calendar provider stores all-day events at UTC midnight, this converts such a timestamp to
 * midnight of the same date in the given zone.
 */
internal fun utcMidnightToLocal(time: Long, zone: ZoneId): Long =
    time.toLocalDate(ZoneOffset.UTC).startMillis(zone)

/**
 * Groups the calendar instances into the days on which they should be shown.
 *
 * @param now the current time
 * @param lookAheadEnd events starting after this point in time are ignored
 * @param multiDayOnEveryDay whether events lasting several days should be shown on each of those
 * days or only on the first one
 * @param skipPassed whether events which already ended should be hidden
 */
internal fun buildAgenda(
    instances: List<Instance>,
    now: Long,
    zone: ZoneId,
    lookAheadEnd: Long,
    multiDayOnEveryDay: Boolean,
    skipPassed: Boolean,
): Agenda {
    val today = now.toLocalDate(zone)
    val todayStart = today.startMillis(zone)
    var nextChange = Long.MAX_VALUE
    val events = ArrayList<Event>(instances.size)

    for (instance in instances) {
        val begin = instance.begin
        val end = instance.end
        if (instance.declined || begin > lookAheadEnd) continue
        // ended before today
        if (end < todayStart || (end == todayStart && begin < todayStart)) continue
        if (end > now) nextChange = minOf(nextChange, end)
        if (skipPassed && end in 1 until now) continue

        val beginDay = begin.toLocalDate(zone)
        // an event ending exactly at midnight does not last into the next day
        val lastDay = (if (end > begin) end - 1 else begin).toLocalDate(zone)
        val multiDay = end > 0 && if (instance.allDay) end - begin > DAY_MS else beginDay != lastDay

        fun event(date: Long, allDay: Boolean, multiDay: Boolean, original: Boolean) = Event(
            id = instance.eventId, date = date, title = instance.title,
            location = instance.location?.takeIf { it.isNotEmpty() }, allDay = allDay,
            end = if (allDay) end - 1 else end, color = instance.color, multiDay = multiDay,
            multiDayIsOriginal = original,
        )

        if (!multiDayOnEveryDay || begin > now ||
            ((!skipPassed || end > now) && beginDay == today)
        ) {
            if (beginDay < today) {
                // started before today: show it on today, like an all-day event
                val movedBegin = Instant.ofEpochMilli(begin).atZone(zone).with(today)
                    .toInstant().toEpochMilli()
                val movedMultiDay = end > 0 &&
                        if (instance.allDay) end - begin > DAY_MS else today != lastDay
                events += event(movedBegin, allDay = true, movedMultiDay, original = true)
            } else {
                events += event(begin, instance.allDay, multiDay, original = true)
            }
        }

        if (multiDayOnEveryDay && multiDay) {
            var day = beginDay.plusDays(1)
            val until = minOf(end, lookAheadEnd)
            while (day.startMillis(zone) < until) {
                if (day >= today) {
                    events += event(
                        day.startMillis(zone), instance.allDay, multiDay = true, original = false
                    )
                }
                day = day.plusDays(1)
            }
        }
    }

    // passed events first, then by start time
    events.sortWith(compareBy<Event> { !it.isPassed(now) }.thenBy { it.date })

    val days = ArrayList<Day>()
    var currentDate: LocalDate? = null
    var currentEvents = ArrayList<Event>()
    for (event in events) {
        val date = event.date.toLocalDate(zone)
        if (date != currentDate) {
            if (currentEvents.isNotEmpty()) days += Day(currentEvents.first().date, currentEvents)
            currentDate = date
            currentEvents = ArrayList()
        }
        currentEvents += event
    }
    if (currentEvents.isNotEmpty()) days += Day(currentEvents.first().date, currentEvents)

    return Agenda(days, nextChange.takeIf { it != Long.MAX_VALUE })
}
