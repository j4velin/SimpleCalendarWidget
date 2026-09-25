package de.j4velin.calendarWidget.data

/**
 * The selected calendars are stored as a comma separated list of ids, `null` if none is selected
 */
internal object CalendarIds {

    fun parse(value: String?): Set<Long> =
        value?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.toSet() ?: emptySet()

    fun format(ids: Set<Long>): String? = if (ids.isEmpty()) null else ids.joinToString(",")
}
