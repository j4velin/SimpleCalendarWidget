package de.j4velin.calendarWidget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal data class IcsEvent(
    val title: String?,
    val description: String?,
    val location: String?,
    val begin: Long?,
    val end: Long?,
    val allDay: Boolean,
)

/** Minimal parser for the first VEVENT of an iCalendar (.ics) file */
internal object IcsParser {

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val DATE = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun parse(lines: Sequence<String>, defaultZone: ZoneId = ZoneId.systemDefault()): IcsEvent? {
        var inEvent = false
        var found = false
        val properties = HashMap<String, Pair<Map<String, String>, String>>()
        for (line in unfold(lines)) {
            val upper = line.uppercase()
            if (!inEvent) {
                if (upper == "BEGIN:VEVENT") {
                    inEvent = true
                    found = true
                }
                continue
            }
            if (upper == "END:VEVENT") break
            val colon = line.indexOf(':').takeIf { it > 0 } ?: continue
            val nameAndParams = line.substring(0, colon).split(';')
            val params = nameAndParams.drop(1).mapNotNull {
                val eq = it.indexOf('=')
                if (eq > 0) it.substring(0, eq).uppercase() to it.substring(eq + 1).trim('"') else null
            }.toMap()
            properties.putIfAbsent(nameAndParams[0].uppercase(), params to line.substring(colon + 1))
        }
        if (!found) return null

        fun text(name: String) = properties[name]?.second?.let(::unescape)?.takeIf { it.isNotBlank() }
        val start = properties["DTSTART"]?.let { (params, value) -> parseTime(params, value, defaultZone) }
        val end = properties["DTEND"]?.let { (params, value) -> parseTime(params, value, defaultZone) }
        return IcsEvent(
            title = text("SUMMARY"),
            description = text("DESCRIPTION"),
            location = text("LOCATION"),
            begin = start?.first,
            end = end?.first,
            allDay = start?.second ?: false,
        )
    }

    /** Joins lines which were folded (continuation lines start with a space or tab) */
    private fun unfold(lines: Sequence<String>): Sequence<String> = sequence {
        var current: StringBuilder? = null
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                current?.append(line, 1, line.length)
            } else {
                current?.let { yield(it.toString().trimEnd('\r')) }
                current = StringBuilder(line)
            }
        }
        current?.let { yield(it.toString().trimEnd('\r')) }
    }

    private fun unescape(value: String) = value.replace("\\n", "\n").replace("\\N", "\n")
        .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    /** @return the time and whether it is an all-day date */
    private fun parseTime(
        params: Map<String, String>, value: String, defaultZone: ZoneId,
    ): Pair<Long, Boolean>? = try {
        val v = value.trim()
        if (params["VALUE"].equals("DATE", ignoreCase = true) || v.length == 8) {
            LocalDate.parse(v, DATE).atStartOfDay(defaultZone).toInstant().toEpochMilli() to true
        } else {
            val zone = when {
                v.endsWith("Z", ignoreCase = true) -> ZoneOffset.UTC
                params["TZID"] != null -> runCatching { ZoneId.of(params["TZID"]) }
                    .getOrDefault(defaultZone)

                else -> defaultZone
            }
            LocalDateTime.parse(v.removeSuffix("Z").removeSuffix("z"), DATE_TIME).atZone(zone)
                .toInstant().toEpochMilli() to false
        }
    } catch (e: DateTimeParseException) {
        null
    }
}
