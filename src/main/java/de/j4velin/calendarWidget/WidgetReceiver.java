package de.j4velin.calendarWidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Objects;

public class WidgetReceiver extends BroadcastReceiver {

    public final static String OPEN_CALENDAR = "OPEN_CALENDAR";
    public final static String ADD_EVENT = "ADD";
    public final static String UPDATE = "UPDATE";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        final int ID = intent.getIntExtra("widgetID", -1);
        if (BuildConfig.DEBUG)
            Logger.log("WidgetReceiver action=" + intent.getAction() + " widget=" + ID);
        if (Objects.equals(intent.getAction(), ADD_EVENT)) {
            if (prefs.contains("otherApp_" + ID)) {
                try {
                    context.startActivity(
                            Intent.parseUri(prefs.getString("otherApp_" + ID, null), 0)
                                    .setAction(Intent.ACTION_INSERT)
                                    .setData(CalendarContract.Events.CONTENT_URI)
                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                            System.currentTimeMillis())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return;
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) Logger.log(e);
                }
            }
            try {
                context.startActivity(new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                System.currentTimeMillis())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(new Intent(Intent.ACTION_INSERT)
                        .setData(Uri.parse("content://com.android.calendar/events/"))
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                System.currentTimeMillis())
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(
                        new Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.item/event")
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        System.currentTimeMillis())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(
                        new Intent(Intent.ACTION_INSERT).setPackage("com.htc.calendar")
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        System.currentTimeMillis())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(
                        new Intent(Intent.ACTION_INSERT).setPackage("com.motorola.blur.calendar")
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        System.currentTimeMillis())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            Toast.makeText(context, "Calendar app not found", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(OPEN_CALENDAR)) {
            Intent openCalendar = new Intent(Intent.ACTION_VIEW);
            Uri uri;
            if (intent.getExtras() != null) {
                long id = intent.getExtras().getLong("eventid", -1);
                if (id == -1) {
                    Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                    builder.appendPath("time");
                    long time = intent.getExtras().getLong("beginTime", System.currentTimeMillis());
                    ContentUris.appendId(builder, time);
                    uri = builder.build();
                } else {
                    uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                    if (intent.hasExtra("beginTime")) {
                        openCalendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                intent.getExtras()
                                        .getLong("beginTime", System.currentTimeMillis()));
                    }
                    if (intent.hasExtra("endTime")) {
                        openCalendar.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                intent.getExtras().getLong("endTime", System.currentTimeMillis()));
                    }
                }
            } else {
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, System.currentTimeMillis());
                uri = builder.build();
            }
            openCalendar.setData(uri);
            openCalendar.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (prefs.contains("otherApp_" + ID)) {
                try {
                    context.startActivity(
                            Intent.parseUri(prefs.getString("otherApp_" + ID, null), 0).setData(uri)
                                    .setAction(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_LAUNCHER)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return;
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) Logger.log(e);
                }
            }

            try {
                context.startActivity(openCalendar);
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(openCalendar.setClassName("com.android.calendar",
                        "com.android.calendar.AllInOneActivity"));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(openCalendar.setClassName("com.google.android.calendar",
                        "com.android.calendar.AllInOneActivity"));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(openCalendar.setAction(Intent.ACTION_MAIN).setComponent(
                        new ComponentName("com.htc.calendar", "com.htc.calendar.MonthActivity")));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(openCalendar.setAction(Intent.ACTION_MAIN).setComponent(
                        new ComponentName("com.htc.calendar", "com.htc.calendar.AgendaActivity")));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(openCalendar.setAction(Intent.ACTION_MAIN).setComponent(
                        new ComponentName("com.motorola.blur.calendar",
                                "com.motorola.blur.calendar.LaunchActivity")));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }
            try {
                context.startActivity(
                        openCalendar.setData(Uri.parse("content://com.android.calendar/events/")));
                return;
            } catch (Exception e) {
                if (BuildConfig.DEBUG) Logger.log(e);
            }

            Toast.makeText(context, "Calendar app not found", Toast.LENGTH_SHORT).show();
        } else { // UPDATE
            new Thread(new Runnable() {
                public void run() {
                    final AppWidgetManager awm = AppWidgetManager.getInstance(context);
                    if (intent.getAction().equals(UPDATE) &&
                            intent.getExtras().containsKey("widgetID")) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());
                        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH), 0, 0, 1);
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        boolean isMonthWidget = prefs.contains("month_offset_" + ID);
                        // update whole widget?
                        if (isMonthWidget) {
                            if (BuildConfig.DEBUG) Logger.log("updating month widget " + ID);
                            if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 1) {
                                if (BuildConfig.DEBUG)
                                    Logger.log("new month -> update whole widget");
                                awm.updateAppWidget(ID, MonthWidget.updateWidget(ID, context));
                            }
                            awm.notifyAppWidgetViewDataChanged(ID, R.id.days);
                            prefs.edit().putLong("lastDateUpdate_" + ID, System.currentTimeMillis())
                                    .apply();
                        } else if (prefs.contains("today_" + ID) &&
                                prefs.getLong("lastDateUpdate_" + ID, 0) < cal.getTimeInMillis()) {
                            if (BuildConfig.DEBUG) Logger.log("updating whole widget " + ID);
                            awm.updateAppWidget(ID, Widget.updateWidget(ID, context));
                            awm.notifyAppWidgetViewDataChanged(ID, R.id.events);
                            prefs.edit().putLong("lastDateUpdate_" + ID, System.currentTimeMillis())
                                    .apply();
                        } else { // update only events list
                            if (BuildConfig.DEBUG) Logger.log("updating only event list " + ID);
                            awm.notifyAppWidgetViewDataChanged(ID, R.id.events);
                        }
                        Widget.setNextAlarm(prefs, cal.getTimeInMillis(), ID, context);
                    } else if (intent.getAction().equals(Intent.ACTION_PROVIDER_CHANGED) ||
                            intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                            intent.getAction().equals(Intent.ACTION_DATE_CHANGED)) {
                        int[] ids = awm.getAppWidgetIds(new ComponentName(context, Widget.class));
                        for (int id : ids) {
                            boolean isMonthWidget = prefs.contains("month_offset_" + id);
                            if (isMonthWidget) {
                                awm.updateAppWidget(id, MonthWidget.updateWidget(id, context));
                                awm.notifyAppWidgetViewDataChanged(id, R.id.days);
                            } else {
                                awm.updateAppWidget(id, Widget.updateWidget(id, context));
                                awm.notifyAppWidgetViewDataChanged(id, R.id.events);
                            }
                        }
                    }
                }
            }).start();
        }
    }
}
