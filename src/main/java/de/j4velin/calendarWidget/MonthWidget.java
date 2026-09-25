package de.j4velin.calendarWidget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.j4velin.calendarWidget.settings.API24Wrapper;
import de.j4velin.calendarWidget.settings.MonthWidgetConfig;

public class MonthWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
                         final int[] appWidgetIds) {
        if (BuildConfig.DEBUG)
            Logger.log("MonthWidget::onUpdate ids=" + Arrays.asList(appWidgetIds));
        SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        for (final int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, updateWidget(appWidgetId, context));
            if (BuildConfig.DEBUG) Logger.log("Next update for month widget " + appWidgetId + ": " +
                    new Date(Widget.getNextMidnight()).toLocaleString());
            Widget.setNextAlarm(prefs, Widget.getNextMidnight(), appWidgetId, context);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            API24Wrapper.scheduleCalenderUpdateJob(context);
        }
    }

    @SuppressLint("SimpleDateFormat")
    @SuppressWarnings("deprecation")
    public static RemoteViews updateWidget(final int appWidgetId, final Context context) {
        if (BuildConfig.DEBUG) Logger.log("updateWidget month " + appWidgetId);
        SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);

        Widget.setNextAlarm(prefs, Widget.getNextMidnight(), appWidgetId, context);

        PendingIntent openCalendar = PendingIntent.getBroadcast(context, appWidgetId,
                new Intent(context, WidgetReceiver.class).setAction(WidgetReceiver.OPEN_CALENDAR)
                        .putExtra("widgetID", appWidgetId), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews monthview = new RemoteViews(context.getPackageName(), R.layout.monthview);

        Intent intent = new Intent(context, MonthWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        monthview.setRemoteAdapter(appWidgetId, R.id.days, intent);

        PendingIntent intentTemplate;
        if (prefs.getBoolean("openCalendar_" + appWidgetId, true) ||
                prefs.contains("otherApp_" + appWidgetId)) {
            intentTemplate = openCalendar;
        } else {
            intentTemplate = PendingIntent.getBroadcast(context, appWidgetId,
                    new Intent(context, WidgetReceiver.class).setAction(WidgetReceiver.UPDATE)
                            .putExtra("widgetID", appWidgetId), PendingIntent.FLAG_IMMUTABLE);
        }

        monthview.setPendingIntentTemplate(R.id.days, intentTemplate);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, prefs.getInt("month_offset_" + appWidgetId, 0));

        monthview.setTextViewText(R.id.month,
                cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " " +
                        cal.get(Calendar.YEAR));
        monthview.setTextColor(R.id.month,
                prefs.getInt("monthlabel_text_" + appWidgetId, Color.WHITE));

        monthview.setInt(R.id.bg, "setBackgroundColor",
                prefs.getInt("monthwidget_bg_" + appWidgetId, Color.TRANSPARENT));

        monthview.setOnClickPendingIntent(R.id.month, PendingIntent
                .getActivity(context, appWidgetId, new Intent(context, MonthWidgetConfig.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("editId", appWidgetId),
                        PendingIntent.FLAG_IMMUTABLE));

        monthview.setOnClickPendingIntent(R.id.next, PendingIntent
                .getBroadcast(context, appWidgetId,
                        new Intent(context, MonthReceiver.class).setAction(MonthReceiver.NEXT)
                                .putExtra("widgetId", appWidgetId), PendingIntent.FLAG_IMMUTABLE));

        monthview.setOnClickPendingIntent(R.id.prev, PendingIntent
                .getBroadcast(context, appWidgetId,
                        new Intent(context, MonthReceiver.class).setAction(MonthReceiver.PREVIOUS)
                                .putExtra("widgetId", appWidgetId), PendingIntent.FLAG_IMMUTABLE));

        int icon = prefs.getInt("icon_" + appWidgetId, 2);
        monthview.setInt(R.id.next, "setAlpha", prefs.getInt("icon_alpha_" + appWidgetId, 255));
        monthview.setInt(R.id.prev, "setAlpha", prefs.getInt("icon_alpha_" + appWidgetId, 255));
        switch (icon) {
            case 1: // black
                monthview.setImageViewResource(R.id.next, R.drawable.ic_next_black);
                monthview.setImageViewResource(R.id.prev, R.drawable.ic_prev_black);
                break;
            case 2: // white
                monthview.setImageViewResource(R.id.next, R.drawable.ic_next);
                monthview.setImageViewResource(R.id.prev, R.drawable.ic_prev);
                break;
        }

        return monthview;
    }
}
