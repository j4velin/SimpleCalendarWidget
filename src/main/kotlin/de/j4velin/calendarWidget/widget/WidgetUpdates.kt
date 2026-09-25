package de.j4velin.calendarWidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.content.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import de.j4velin.calendarWidget.MonthWidget
import de.j4velin.calendarWidget.UpdaterJob
import de.j4velin.calendarWidget.Widget
import de.j4velin.calendarWidget.WidgetReceiver
import de.j4velin.calendarWidget.data.widgetPrefs
import de.j4velin.calendarWidget.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Scope for work which must outlive the component that started it */
internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Runs [block] asynchronously while keeping the broadcast receiver alive */
internal fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    appScope.launch {
        try {
            block()
        } catch (e: Exception) {
            log(e)
        } finally {
            pending.finish()
        }
    }
}

internal object WidgetUpdates {

    /**
     * Changed whenever a widget should reload its data. Glance only recomposes a running widget
     * session when its state changes, so a plain `update()` call is not enough.
     */
    val RefreshKey = longPreferencesKey("refresh")

    private const val JOB_CALENDAR_CHANGED = 1

    /** Upper bound of the JobScheduler ids used by this app, WorkManager uses the ids above */
    const val JOB_ID_MAX = 1000

    private fun widgetFor(context: Context, appWidgetId: Int): GlanceAppWidget? =
        when (AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className) {
            Widget::class.java.name -> AgendaGlanceWidget()
            MonthWidget::class.java.name -> MonthGlanceWidget()
            else -> null
        }

    suspend fun refresh(context: Context, widget: GlanceAppWidget, glanceId: GlanceId) {
        updateAppWidgetState(context, glanceId) { it[RefreshKey] = (it[RefreshKey] ?: 0) + 1 }
        widget.update(context, glanceId)
    }

    suspend fun refresh(context: Context, appWidgetId: Int) {
        val widget = widgetFor(context, appWidgetId) ?: return
        val glanceId = try {
            GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
        } catch (e: IllegalArgumentException) {
            log(e)
            return
        }
        refresh(context, widget, glanceId)
    }

    fun refreshAsync(context: Context, appWidgetId: Int) {
        val appContext = context.applicationContext
        appScope.launch { refresh(appContext, appWidgetId) }
    }

    suspend fun refreshAll(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(AgendaGlanceWidget::class.java).forEach {
            refresh(context, AgendaGlanceWidget(), it)
        }
        manager.getGlanceIds(MonthGlanceWidget::class.java).forEach {
            refresh(context, MonthGlanceWidget(), it)
        }
    }

    fun nextMidnight(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 1000

    /**
     * Schedules an update of the given widget. If there already is an earlier update scheduled,
     * that one is kept.
     */
    fun scheduleUpdate(context: Context, appWidgetId: Int, time: Long) {
        val now = System.currentTimeMillis()
        if (time <= now) return
        val prefs = context.widgetPrefs()
        val scheduled = prefs.getLong("nextUpdate_$appWidgetId", 0)
        val next = if (scheduled > now) minOf(time, scheduled) else time
        val intent = Intent(context, WidgetReceiver::class.java)
            .setAction(WidgetReceiver.UPDATE)
            .putExtra(WidgetReceiver.EXTRA_WIDGET_ID, appWidgetId)
        context.getSystemService(AlarmManager::class.java).set(
            AlarmManager.RTC, next, PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        prefs.edit { putLong("nextUpdate_$appWidgetId", next) }
        log("next update for widget $appWidgetId at $next")
    }

    /** Schedules a job which updates all widgets as soon as the calendar data changes */
    fun scheduleCalendarObserver(context: Context) {
        val job = JobInfo.Builder(
            JOB_CALENDAR_CHANGED, ComponentName(context, UpdaterJob::class.java)
        ).addTriggerContentUri(
            JobInfo.TriggerContentUri(
                CalendarContract.Instances.CONTENT_URI,
                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
            )
        ).setTriggerContentUpdateDelay(1000).build()
        if (context.getSystemService(JobScheduler::class.java)
                .schedule(job) != JobScheduler.RESULT_SUCCESS
        ) {
            log("error scheduling calendar observer job")
        }
    }
}
