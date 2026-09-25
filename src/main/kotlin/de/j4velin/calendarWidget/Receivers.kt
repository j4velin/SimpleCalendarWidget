package de.j4velin.calendarWidget

import android.app.job.JobParameters
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import de.j4velin.calendarWidget.data.widgetPrefs
import de.j4velin.calendarWidget.widget.AgendaGlanceWidget
import de.j4velin.calendarWidget.widget.MonthGlanceWidget
import de.j4velin.calendarWidget.widget.WidgetUpdates
import de.j4velin.calendarWidget.widget.appScope
import de.j4velin.calendarWidget.widget.launchAsync
import kotlinx.coroutines.launch

// The class names of the widget providers must not change, otherwise already placed widgets break.

/** The agenda widget */
class Widget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaGlanceWidget()

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        onWidgetsUpdated(context, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        onWidgetsDeleted(context, appWidgetIds)
    }
}

/** The month widget */
class MonthWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthGlanceWidget()

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        onWidgetsUpdated(context, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        onWidgetsDeleted(context, appWidgetIds)
    }
}

private fun onWidgetsUpdated(context: Context, appWidgetIds: IntArray) {
    WidgetUpdates.scheduleCalendarObserver(context)
    // GlanceAppWidgetReceiver already holds the receiver alive via goAsync(), which can only be
    // called once. Force a data reload in case a widget session is still running.
    val appContext = context.applicationContext
    appScope.launch { appWidgetIds.forEach { WidgetUpdates.refresh(appContext, it) } }
}

private fun onWidgetsDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach { WidgetUpdates.cancelUpdates(context, it) }
    val prefs = context.widgetPrefs()
    val suffixes = appWidgetIds.map { "_$it" }
    prefs.edit {
        prefs.all.keys.filter { key -> suffixes.any { key.endsWith(it) } }.forEach { remove(it) }
    }
}

/** Updates the widgets when a scheduled update is due or the calendar data / time changed */
class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        log("WidgetReceiver action=${intent.action}")
        when (intent.action) {
            UPDATE -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    launchAsync { WidgetUpdates.refresh(context, widgetId) }
                }
            }

            // alarms and jobs are cleared on reboot, reloading the widgets schedules them again
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetUpdates.scheduleCalendarObserver(context)
                launchAsync { WidgetUpdates.refreshAll(context) }
            }
        }
    }

    companion object {
        const val UPDATE = "UPDATE"
        const val EXTRA_WIDGET_ID = "widgetID"
    }
}

/** Triggered by the JobScheduler whenever the calendar data changes */
class UpdaterJob : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        log("UpdaterJob started")
        appScope.launch {
            WidgetUpdates.refreshAll(this@UpdaterJob)
            jobFinished(params, false)
            // content trigger jobs only fire once
            WidgetUpdates.scheduleCalendarObserver(this@UpdaterJob)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
