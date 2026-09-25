package de.j4velin.calendarWidget.widget

import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.DAY_MS
import de.j4velin.calendarWidget.data.Day
import de.j4velin.calendarWidget.data.Event
import java.text.SimpleDateFormat
import java.util.Locale

/** Formats a timestamp with a user defined pattern, falling back to a default pattern if invalid */
internal class SafeDateFormat(pattern: String, fallback: String, locale: Locale) {
    private val format = try {
        SimpleDateFormat(pattern, locale)
    } catch (e: IllegalArgumentException) {
        SimpleDateFormat(fallback, locale)
    }

    fun format(time: Long): String = format.format(time)

    companion object {
        fun isValid(pattern: String): Boolean = try {
            SimpleDateFormat(pattern, Locale.getDefault()).format(System.currentTimeMillis())
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

/** Creates the texts shown in the agenda widget */
internal class AgendaFormatter(
    private val settings: AgendaSettings,
    defaultTimeFormat: String,
    defaultDateFormat: String,
    defaultAlldayEndFormat: String,
    locale: Locale = Locale.getDefault(),
) {
    private val dateFormat = SafeDateFormat(settings.dateFormat, defaultDateFormat, locale)
    private val timeFormat = SafeDateFormat(settings.timeFormat, defaultTimeFormat, locale)
    private val alldayEndFormat =
        SafeDateFormat(settings.alldayEndFormat, defaultAlldayEndFormat, locale)

    /**
     * @param relativeDay 0 for today, 1 for tomorrow
     * @param today the localized word for 'today'
     * @param tomorrow the localized word for 'tomorrow'
     */
    fun dayTitle(day: Day, relativeDay: Long, today: String, tomorrow: String): String = when {
        settings.todayTomorrowText && relativeDay == 0L -> today
        settings.todayTomorrowText && relativeDay == 1L -> tomorrow
        else -> dateFormat.format(day.date)
    }

    /** @return the time label shown in front of the event's title, or null if there is none */
    fun timeText(event: Event, day: Day): String? {
        val showEndDate = !settings.multiDayOnEveryDay
        return if (!event.allDay) {
            if (!event.multiDay || event.multiDayIsOriginal) {
                buildString {
                    append(timeFormat.format(event.date)).append(' ')
                    if (event.multiDay && showEndDate) {
                        append("» ").append(alldayEndFormat.format(event.end)).append(' ')
                    }
                    if (event.end > 0 && settings.showEndTimes && (!event.multiDay || showEndDate)) {
                        append("- ").append(timeFormat.format(event.end)).append(' ')
                    }
                }
            } else if (event.end < day.date + DAY_MS) {
                // last day of a multi-day event
                "» " + timeFormat.format(event.end) + " "
            } else {
                null
            }
        } else if (event.multiDay && showEndDate) {
            "» " + alldayEndFormat.format(event.end) + " "
        } else {
            null
        }
    }
}
