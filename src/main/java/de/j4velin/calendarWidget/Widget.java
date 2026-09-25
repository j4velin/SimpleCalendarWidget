package de.j4velin.calendarWidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.PermissionChecker;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import de.j4velin.calendarWidget.settings.API24Wrapper;
import de.j4velin.calendarWidget.settings.Fragment_Appearance;
import de.j4velin.calendarWidget.settings.WidgetConfig;

public class Widget extends AppWidgetProvider {

    static void setNextAlarm(final SharedPreferences prefs, final long zeit, final int appWidgetId,
                             final Context context) {
        if (BuildConfig.DEBUG) Logger.log(
                "Widget::setNextAlarm " + appWidgetId + " " + new Date(zeit).toLocaleString());
        if (zeit > System.currentTimeMillis()) {
            long nextUpdatePrefs = prefs.getLong("nextUpdate_" + appWidgetId, 0);
            long nextUpdate = zeit;
            if (nextUpdatePrefs > System.currentTimeMillis()) {
                nextUpdate = Math.min(nextUpdate, nextUpdatePrefs);
            }
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                    .set(AlarmManager.RTC, nextUpdate, PendingIntent
                            .getBroadcast(context, appWidgetId,
                                    new Intent(context, WidgetReceiver.class)
                                            .setAction(WidgetReceiver.UPDATE)
                                            .putExtra("widgetID", appWidgetId),
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            prefs.edit().putLong("nextUpdate_" + appWidgetId, nextUpdate).apply();
            if (BuildConfig.DEBUG) Logger.log("Next update for widget " + appWidgetId + ": " +
                    new Date(nextUpdate).toLocaleString());
        } else if (BuildConfig.DEBUG) {
            Logger.log("setNextAlarm - invalid time argument: " + zeit + " currently: " +
                    System.currentTimeMillis());
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
                         final int[] appWidgetIds) {
        if (BuildConfig.DEBUG) Logger.log("Widget::onUpdate ids=" + Arrays.asList(appWidgetIds));
        SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        for (final int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, updateWidget(appWidgetId, context));
            setNextAlarm(prefs, getNextMidnight(), appWidgetId, context);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            API24Wrapper.scheduleCalenderUpdateJob(context);
        }
    }

    /**
     * @return the ids of all agenda and month widgets
     */
    static int[] getAllWidgetIds(final Context context) {
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        int[] agenda = awm.getAppWidgetIds(new ComponentName(context, Widget.class));
        int[] month = awm.getAppWidgetIds(new ComponentName(context, MonthWidget.class));
        int[] all = Arrays.copyOf(agenda, agenda.length + month.length);
        System.arraycopy(month, 0, all, agenda.length, month.length);
        return all;
    }

    public static long getNextMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0,
                0, 1);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    @SuppressLint("SimpleDateFormat")
    @SuppressWarnings("deprecation")
    public static RemoteViews updateWidget(final int appWidgetId, final Context context) {
        if (BuildConfig.DEBUG) Logger.log("updateWidget " + appWidgetId);

        SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);

        setNextAlarm(prefs, getNextMidnight(), appWidgetId, context);

        // must be mutable as it is also used as template for the collection items' fill-in intents
        PendingIntent openCalendar = PendingIntent.getBroadcast(context, appWidgetId,
                new Intent(context, WidgetReceiver.class).setAction(WidgetReceiver.OPEN_CALENDAR)
                        .putExtra("widgetID", appWidgetId), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        int layout = prefs.getInt("layout_" + appWidgetId, WidgetService.LAYOUT_NONE_LIGHT);
        RemoteViews views;
        if (layout == WidgetService.LAYOUT_NONE_LIGHT || layout == WidgetService.LAYOUT_TEXT_LIGHT)
            views = new RemoteViews(context.getPackageName(), R.layout.widget);
        else views = new RemoteViews(context.getPackageName(), R.layout.widgetlight);

        if (prefs.getString("today_" + appWidgetId, null) != null) {
            CharSequence cs;
            try {
                cs = new SimpleDateFormat(prefs.getString("today_" + appWidgetId,
                        Fragment_Appearance.DEFAULT_TODAY_DATE_FORMAT))
                        .format(System.currentTimeMillis());
            } catch (Exception ex) {
                cs = new SimpleDateFormat(Fragment_Appearance.DEFAULT_TODAY_DATE_FORMAT)
                        .format(System.currentTimeMillis());
            }
            if (prefs.getBoolean("current_date_bold_" + appWidgetId, false)) {
                SpannableString s = new SpannableString(cs);
                s.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
                cs = s;
            }
            views.setViewVisibility(R.id.today, View.VISIBLE);
            views.setTextViewText(R.id.today, cs);
            views.setTextColor(R.id.today, prefs.getInt("current_date_color_" + appWidgetId,
                    Fragment_Appearance.DEFAULT_DATE_COLOR));
            views.setFloat(R.id.today, "setTextSize",
                    prefs.getFloat("current_date_size_" + appWidgetId,
                            Fragment_Appearance.DEFAULT_DATE_SIZE));
            views.setOnClickPendingIntent(R.id.today, openCalendar);
        } else {
            views.setViewVisibility(R.id.today, View.GONE);
        }
        views.setOnClickPendingIntent(R.id.add, PendingIntent.getBroadcast(context, appWidgetId,
                new Intent(context, WidgetReceiver.class).setAction(WidgetReceiver.ADD_EVENT)
                        .putExtra("widgetID", appWidgetId), PendingIntent.FLAG_IMMUTABLE));

        views.setOnClickPendingIntent(R.id.edit, PendingIntent.getActivity(context, appWidgetId,
                new Intent(context, WidgetConfig.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("editId", appWidgetId), PendingIntent.FLAG_IMMUTABLE));

        if (Build.VERSION.SDK_INT >= 23 &&
                PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                        PermissionChecker.PERMISSION_DENIED) {
            views.setTextViewText(R.id.empty, "No permission to read from Calendar");
            views.setViewVisibility(R.id.empty, View.VISIBLE);
            views.setOnClickPendingIntent(R.id.empty, PendingIntent
                    .getActivity(context, appWidgetId, new Intent(context, WidgetConfig.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("editId", appWidgetId), PendingIntent.FLAG_IMMUTABLE));
        } else {
            Intent intent = new Intent(context, WidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            views.setRemoteAdapter(appWidgetId, R.id.events, intent);

            PendingIntent intentTemplate;
            if (prefs.getBoolean("openCalendar_" + appWidgetId, true) ||
                    prefs.contains("otherApp_" + appWidgetId)) {
                intentTemplate = openCalendar;
            } else {
                intentTemplate = PendingIntent.getBroadcast(context, appWidgetId,
                        new Intent(context, WidgetReceiver.class).setAction(WidgetReceiver.UPDATE)
                                .putExtra("widgetID", appWidgetId), PendingIntent.FLAG_IMMUTABLE);
            }

            views.setPendingIntentTemplate(R.id.events, intentTemplate);

            views.setEmptyView(R.id.events, R.id.empty);
            views.setOnClickPendingIntent(R.id.empty, openCalendar);
        }

        views.setInt(R.id.bg, "setBackgroundColor",
                prefs.getInt("bg_" + appWidgetId, Fragment_Appearance.DEFAULT_BG_COLOR));

        int icon = prefs.getInt("icon_" + appWidgetId, 2);
        views.setInt(R.id.add, "setAlpha", prefs.getInt("icon_alpha_" + appWidgetId, 255));
        views.setInt(R.id.edit, "setAlpha", prefs.getInt("icon_alpha_" + appWidgetId, 255));
        switch (icon) {
            case 1: // black
                views.setImageViewResource(R.id.add, R.drawable.ic_add_black);
                views.setImageViewResource(R.id.edit, R.drawable.ic_settings_black);
                break;
            case 2: // white
                views.setImageViewResource(R.id.add, R.drawable.ic_add_white);
                views.setImageViewResource(R.id.edit, R.drawable.ic_settings_white);
                break;
        }
        return views;
    }
}
