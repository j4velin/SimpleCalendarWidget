package de.j4velin.calendarWidget.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import de.j4velin.calendarWidget.log
import java.time.ZoneId

internal data class CalendarInfo(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
    /** whether the user has chosen to show this calendar in the calendar app */
    val visible: Boolean,
)

/** Read access to the system calendar provider */
internal class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CALENDAR
    ) == PackageManager.PERMISSION_GRANTED

    fun calendars(): List<CalendarInfo> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
        )
        return context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarInfo(
                            id = cursor.getLong(0),
                            name = cursor.getString(1).orEmpty(),
                            account = cursor.getString(2).orEmpty(),
                            color = cursor.getInt(3),
                            visible = cursor.getInt(4) == 1,
                        )
                    )
                }
            }
        } ?: emptyList()
    }

    /**
     * @return all event instances of the given calendars which overlap the given time range.
     * The times of all-day events are converted from UTC to the given zone
     */
    fun instances(
        calendarIds: Set<Long>,
        from: Long,
        to: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Instance> {
        if (calendarIds.isEmpty() || !hasPermission()) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN " +
                "(${calendarIds.joinToString(",")}) AND " +
                "${CalendarContract.Instances.TITLE} NOT NULL AND " +
                "deleted != 1" // CalendarContract.SyncColumns.DELETED, not accessible from Kotlin
        return try {
            context.contentResolver.query(
                uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val allDay = cursor.getInt(3) == 1
                        var begin = cursor.getLong(4)
                        var end = cursor.getLong(5)
                        if (allDay) {
                            begin = utcMidnightToLocal(begin, zone)
                            end = utcMidnightToLocal(end, zone)
                        }
                        add(
                            Instance(
                                eventId = cursor.getLong(0),
                                title = cursor.getString(1).orEmpty(),
                                location = cursor.getString(2),
                                allDay = allDay,
                                begin = begin,
                                end = end,
                                color = cursor.getInt(6),
                                declined = cursor.getInt(7) ==
                                        CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (e: SecurityException) {
            log(e)
            emptyList()
        }
    }
}
