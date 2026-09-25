package de.j4velin.calendarWidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.content.PermissionChecker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MonthWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new ListViewFactory(this, intent);
    }

    class ListViewFactory implements RemoteViewsFactory {

        private List<RemoteViews> days;
        private final Context context;
        private final int widgetId;

        private ListViewFactory(final Context context, final Intent intent) {
            this.context = context;
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (days == null || position >= days.size()) return null;
            return days.get(position);
        }

        @Override
        public void onCreate() {
            if (BuildConfig.DEBUG) Logger.log("MonthWidgetService::onCreate " + widgetId);
        }

        @SuppressLint({"SimpleDateFormat", "NewApi"})
        @Override
        public void onDataSetChanged() {
            if (BuildConfig.DEBUG) Logger.log("MonthWidgetService::onDataSetChanged " + widgetId);
            final SharedPreferences prefs =
                    getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
            final String cals = prefs.getString("cals_" + widgetId, null);

            days = new ArrayList<>(49);
            RemoteViews dayItem;

            Calendar currentDay = Calendar.getInstance();
            int today = currentDay.get(Calendar.DAY_OF_YEAR);

            currentDay.add(Calendar.MONTH, prefs.getInt("month_offset_" + widgetId, 0));

            int thisMonth = currentDay.get(Calendar.MONTH);

            currentDay.set(Calendar.DAY_OF_MONTH, 1);
            currentDay.set(Calendar.HOUR_OF_DAY, 0);
            currentDay.set(Calendar.MINUTE, 0);
            currentDay.set(Calendar.SECOND, 1);

            boolean startOnMonday = prefs.getBoolean("start_monday_" + widgetId,
                    !Locale.getDefault().getCountry().equalsIgnoreCase(Locale.US.getCountry()));

            if (!startOnMonday) {
                currentDay.add(Calendar.DAY_OF_YEAR,
                        -((currentDay.get(Calendar.DAY_OF_WEEK) + 6) % 7));
            } else {
                currentDay.add(Calendar.DAY_OF_YEAR,
                        -((currentDay.get(Calendar.DAY_OF_WEEK) + 5) % 7));
            }

            if (currentDay.get(Calendar.DAY_OF_MONTH) == 1) {
                currentDay.add(Calendar.DAY_OF_YEAR, -7);
            }

            for (int day = 0; day < 7; day++) {
                dayItem = new RemoteViews(context.getPackageName(), R.layout.monthday);
                dayItem.setTextViewText(R.id.day, currentDay
                        .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                        .toUpperCase());
                dayItem.setTextColor(R.id.day,
                        prefs.getInt("dayslabel_text_" + widgetId, Color.LTGRAY));
                dayItem.setInt(R.id.day, "setBackgroundColor", Color.TRANSPARENT);
                currentDay.add(Calendar.DAY_OF_YEAR, 1);
                days.add(dayItem);
            }
            currentDay.add(Calendar.DAY_OF_YEAR, -7);

            TimeZone tz = TimeZone.getDefault();

            int bgColor, textColor;

            boolean hasPermission;
            try {
                hasPermission = Build.VERSION.SDK_INT < 23 || PermissionChecker
                        .checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
                        PermissionChecker.PERMISSION_DENIED;
            } catch (SecurityException se) {
                // no permission to check permission?!? try through packagemanager then
                hasPermission = context.getPackageManager()
                        .checkPermission(Manifest.permission.READ_CALENDAR,
                                context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
            }

            for (int week = 1; week <= 6; week++) {
                for (int day = 0; day < 7; day++) {
                    long start = currentDay.getTimeInMillis();
                    dayItem = new RemoteViews(context.getPackageName(), R.layout.monthday);
                    dayItem.setTextViewText(R.id.day,
                            String.valueOf(currentDay.get(Calendar.DAY_OF_MONTH)));
                    if (currentDay.get(Calendar.DAY_OF_YEAR) == today) {
                        bgColor = prefs.getInt("today_bg_" + widgetId, Color.WHITE);
                        textColor = prefs.getInt("today_text_" + widgetId, Color.BLACK);
                    } else if (currentDay.get(Calendar.MONTH) == thisMonth) {
                        bgColor =
                                prefs.getInt("month_bg_" + widgetId, Color.argb(75, 255, 255, 255));
                        textColor = prefs.getInt("month_text_" + widgetId, Color.WHITE);
                    } else {
                        bgColor = prefs.getInt("other_bg_" + widgetId, Color.TRANSPARENT);
                        textColor = prefs.getInt("other_text_" + widgetId, Color.LTGRAY);
                    }

                    dayItem.setInt(R.id.day, "setBackgroundColor", bgColor);
                    dayItem.setTextColor(R.id.day, textColor);

                    dayItem.setOnClickFillInIntent(R.id.day,
                            new Intent().putExtra("beginTime", start));

                    currentDay.add(Calendar.DAY_OF_YEAR, 1);

                    if (cals != null && hasPermission) {
                        long end = currentDay.getTimeInMillis() - 2000;
                        long tzNow = start + tz.getOffset(start);
                        long tzMax = end + tz.getOffset(end);

                        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
                        ContentUris.appendId(builder, tzNow);
                        ContentUris.appendId(builder, tzMax);

                        Cursor cursor = getContentResolver().query(builder.build(),
                                new String[]{CalendarContract.Instances.EVENT_ID},
                                CalendarContract.Events.CALENDAR_ID + " IN (" + cals + ") AND " +
                                        CalendarContract.Events.TITLE + " NOT NULL AND " +
                                        CalendarContract.Events.DELETED + " != 1", null, null);
                        if (cursor != null) {
                            if (cursor.getCount() > 0) {
                                cursor.moveToFirst();
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < cursor.getCount(); i++) sb.append(".");
                                dayItem.setTextViewText(R.id.event, sb.toString());
                                dayItem.setTextColor(R.id.event, textColor);
                                dayItem.setViewVisibility(R.id.event, View.VISIBLE);
                            } else {
                                dayItem.setViewVisibility(R.id.event, View.GONE);
                            }
                            cursor.close();
                        } else {
                            dayItem.setViewVisibility(R.id.event, View.GONE);
                        }
                    }

                    days.add(dayItem);
                }
            }
        }

        @Override
        public int getCount() {
            return 49;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDestroy() {
            if (BuildConfig.DEBUG) Logger.log("MonthWidgetService::onDestroy " + widgetId);
        }

    }

}
