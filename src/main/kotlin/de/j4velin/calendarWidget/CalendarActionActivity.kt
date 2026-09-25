package de.j4velin.calendarWidget

import android.app.Activity
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.net.toUri
import de.j4velin.calendarWidget.data.widgetPrefs

/**
 * Invisible activity started by the widgets to open the calendar app or to add a new event.
 *
 * Starting other apps from a broadcast receiver is blocked by the background activity start
 * restrictions, so the widgets start this activity instead, which then tries several ways to
 * reach a calendar app.
 */
class CalendarActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(WidgetReceiver.EXTRA_WIDGET_ID, -1)
        val otherApp = widgetPrefs().getString("otherApp_$widgetId", null)
        val started = when (intent.action) {
            ACTION_ADD_EVENT -> addEvent(otherApp)
            else -> openCalendar(otherApp)
        }
        if (!started) Toast.makeText(this, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun tryStart(vararg candidates: () -> Intent): Boolean = candidates.any {
        try {
            startActivity(it().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            log(e)
            false
        }
    }

    private fun addEvent(otherApp: String?): Boolean {
        val now = System.currentTimeMillis()
        fun Intent.withBegin() = putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, now)
        val insert = { Intent(Intent.ACTION_INSERT).withBegin() }
        val otherAppIntents: Array<() -> Intent> = if (otherApp != null) arrayOf({
            Intent.parseUri(otherApp, 0).setAction(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI).withBegin()
        }) else emptyArray()
        return tryStart(
            *otherAppIntents,
            { insert().setData(CalendarContract.Events.CONTENT_URI) },
            { insert().setType("vnd.android.cursor.item/event") },
        )
    }

    private fun openCalendar(otherApp: String?): Boolean {
        val extras = intent.extras
        val eventId = extras?.getLong(EXTRA_EVENT_ID, -1) ?: -1
        val view = Intent(Intent.ACTION_VIEW)
        val uri: Uri = if (eventId == -1L) {
            val time = extras?.getLong(EXTRA_BEGIN_TIME, System.currentTimeMillis())
                ?: System.currentTimeMillis()
            CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                .also { ContentUris.appendId(it, time) }.build()
        } else {
            if (intent.hasExtra(EXTRA_BEGIN_TIME)) {
                view.putExtra(
                    CalendarContract.EXTRA_EVENT_BEGIN_TIME, intent.getLongExtra(EXTRA_BEGIN_TIME, 0)
                )
            }
            if (intent.hasExtra(EXTRA_END_TIME)) {
                view.putExtra(
                    CalendarContract.EXTRA_EVENT_END_TIME, intent.getLongExtra(EXTRA_END_TIME, 0)
                )
            }
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        }
        view.data = uri

        val otherAppIntents: Array<() -> Intent> = if (otherApp != null) arrayOf({
            Intent.parseUri(otherApp, 0).setData(uri).setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
        }) else emptyArray()
        return tryStart(
            *otherAppIntents,
            { Intent(view) },
            { Intent(view).setClassName("com.android.calendar", ALL_IN_ONE_ACTIVITY) },
            { Intent(view).setClassName("com.google.android.calendar", ALL_IN_ONE_ACTIVITY) },
            { Intent(Intent.ACTION_VIEW, "content://com.android.calendar/events/".toUri()) },
        )
    }

    companion object {
        private const val ACTION_OPEN_CALENDAR = "OPEN_CALENDAR"
        private const val ACTION_ADD_EVENT = "ADD"
        private const val EXTRA_BEGIN_TIME = "beginTime"
        private const val EXTRA_END_TIME = "endTime"
        private const val EXTRA_EVENT_ID = "eventid"
        private const val ALL_IN_ONE_ACTIVITY = "com.android.calendar.AllInOneActivity"

        private fun intent(context: Context, action: String, widgetId: Int) =
            Intent(context, CalendarActionActivity::class.java).setAction(action)
                .putExtra(WidgetReceiver.EXTRA_WIDGET_ID, widgetId)

        /** Opens the calendar app at the given time, or now if null */
        fun openCalendar(context: Context, widgetId: Int, time: Long? = null): Intent =
            intent(context, ACTION_OPEN_CALENDAR, widgetId).apply {
                if (time != null) putExtra(EXTRA_BEGIN_TIME, time)
            }

        /** Opens the given event in the calendar app */
        fun openEvent(
            context: Context, widgetId: Int, eventId: Long, begin: Long?, end: Long?,
        ): Intent = intent(context, ACTION_OPEN_CALENDAR, widgetId).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            if (begin != null) putExtra(EXTRA_BEGIN_TIME, begin)
            if (end != null) putExtra(EXTRA_END_TIME, end)
        }

        fun addEvent(context: Context, widgetId: Int): Intent =
            intent(context, ACTION_ADD_EVENT, widgetId)
    }
}
