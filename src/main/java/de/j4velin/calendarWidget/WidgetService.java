package de.j4velin.calendarWidget;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.j4velin.calendarWidget.settings.Fragment_Appearance;
import de.j4velin.calendarWidget.settings.Fragment_Settings;

public class WidgetService extends RemoteViewsService {

    public final static int LAYOUT_NONE_LIGHT = 0;
    public final static int LAYOUT_TITLE_LIGHT = 1;
    public final static int LAYOUT_TEXT_LIGHT = 2;
    public final static int LAYOUT_ALL_LIGHT = 3;

    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new ListViewFactory(this, intent);
    }

    @SuppressWarnings("MissingPermission")
            // permission is checked in Widget class before loading this class
    class ListViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private List<RemoteViews> views;
        private final Context context;
        private final int widgetId;
        private final SharedPreferences prefs;

        public ListViewFactory(final Context context, final Intent intent) {
            this.context = context;
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            prefs = context.getSharedPreferences("calendarWidget", MODE_PRIVATE);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (views == null || position >= views.size()) return null;
            return views.get(position);
        }

        @Override
        public void onCreate() {
            if (BuildConfig.DEBUG) Logger.log("WidgetService::onCreate " + widgetId);
        }

        @SuppressLint({"SimpleDateFormat", "NewApi"})
        @Override
        public void onDataSetChanged() {
            if (BuildConfig.DEBUG) Logger.log("WidgetService::onDataSetChanged " + widgetId);
            List<Day> days;
            try {
                days = Parser.parse(context, widgetId);
            } catch (SecurityException se) {
                if (BuildConfig.DEBUG) Logger.log(se);
                return;
            }
            if (days == null || days.isEmpty()) {
                views = null;
                return;
            }
            views = new ArrayList<>(days.size());
            RemoteViews dayItem;
            RemoteViews eventItem;
            CharSequence cs;
            int layout = prefs.getInt("layout_" + widgetId, LAYOUT_TEXT_LIGHT);

            boolean showColors = prefs.getBoolean("showCalendarColors_" + widgetId, false);
            boolean openEvent = prefs.getBoolean("openEvent_" + widgetId, false);
            boolean singleLineEvent = prefs.getBoolean("singleLineEvent_" + widgetId, false);
            boolean singleLineToday = prefs.getBoolean("singleLineToday_" + widgetId, false);
            boolean singleLineTodayPassed =
                    prefs.getBoolean("singleLineTodayPassed_" + widgetId, false);
            boolean singleLineLocation = prefs.getBoolean("singleLineLocation_" + widgetId, false);

            String alldayEndFormat = prefs.getString("allday_endformat_" + widgetId, "");

            for (Day day : days) {

                if (layout == LAYOUT_NONE_LIGHT || layout == LAYOUT_TEXT_LIGHT)
                    dayItem = new RemoteViews(context.getPackageName(), R.layout.dayitem);
                else dayItem = new RemoteViews(context.getPackageName(), R.layout.dayitemlight);

                cs = null;

                if (prefs.getBoolean("todayText_" + widgetId, true)) {
                    if (DateUtils.isToday(day.date)) cs = getString(R.string.today);
                    else if (DateUtils.isToday(day.date - 86400000))
                        cs = getString(R.string.tomorrow);
                }

                if (cs == null) {
                    try {
                        cs = new SimpleDateFormat(prefs.getString("dateformat_" + widgetId,
                                Fragment_Appearance.DEFAULT_DATE_FORMAT)).format(day.date);
                    } catch (Exception ex) {
                        cs = new SimpleDateFormat(Fragment_Appearance.DEFAULT_DATE_FORMAT)
                                .format(day.date);
                    }
                }

                if (prefs.getBoolean("datebold_" + widgetId, true)) {
                    SpannableString s = new SpannableString(cs);
                    s.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
                    cs = s;
                }

                dayItem.setTextViewText(R.id.title, cs);
                dayItem.setTextColor(R.id.title,
                        prefs.getInt("title_" + widgetId, Fragment_Appearance.DEFAULT_DATE_COLOR));
                dayItem.setFloat(R.id.title, "setTextSize", prefs.getFloat("titlesize_" + widgetId,
                        Fragment_Appearance.DEFAULT_DATE_SIZE));

                dayItem.removeAllViews(R.id.events);

                for (Event e : day.events) {

                    if (layout == LAYOUT_NONE_LIGHT || layout == LAYOUT_TITLE_LIGHT)
                        eventItem = new RemoteViews(context.getPackageName(), R.layout.eventitem);
                    else eventItem =
                            new RemoteViews(context.getPackageName(), R.layout.eventitemlight);

                    if (!showColors) {
                        eventItem.setViewVisibility(R.id.color, View.GONE);
                    } else {
                        eventItem.setInt(R.id.color, "setBackgroundColor", e.color);
                    }

                    cs = e.title;
                    if (prefs.getBoolean("eventbold_" + widgetId, false)) {
                        SpannableString s = new SpannableString(cs);
                        s.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
                        cs = s;
                    }

                    int eventColor, timeColor, locationColor;
                    float eventSize, timeSize, locationSize;

                    timeColor = prefs.getInt("time_" + widgetId,
                            Fragment_Appearance.DEFAULT_TIME_COLOR);
                    timeSize = prefs.getFloat("timesize_" + widgetId,
                            Fragment_Appearance.DEFAULT_TIME_SIZE);
                    locationColor = prefs.getInt("location_" + widgetId,
                            Fragment_Appearance.DEFAULT_LOCATION_COLOR);
                    locationSize = prefs.getFloat("locationsize_" + widgetId,
                            Fragment_Appearance.DEFAULT_LOCATION_SIZE);

                    if (DateUtils.isToday(day.date)) {
                        if (e.end < System.currentTimeMillis()) {
                            // already passed
                            if (singleLineTodayPassed) {
                                eventItem.setInt(R.id.title, "setLines", 1);
                            }
                            eventColor = prefs.getInt("today_passed_text_" + widgetId,
                                    Fragment_Appearance.DEFAULT_PASSED_COLOR);
                            eventSize = prefs.getFloat("today_passed_textsize_" + widgetId,
                                    Fragment_Appearance.DEFAULT_EVENT_SIZE);
                        } else {
                            if (singleLineToday) {
                                eventItem.setInt(R.id.title, "setLines", 1);
                            }
                            eventColor = prefs.getInt("today_text_" + widgetId,
                                    Fragment_Appearance.DEFAULT_EVENT_COLOR);
                            eventSize = prefs.getFloat("today_textsize_" + widgetId,
                                    Fragment_Appearance.DEFAULT_TODAY_SIZE);
                        }
                        if (prefs.getBoolean("timeLocationTodaySameAsEvent_" + widgetId, true)) {
                            timeColor = eventColor;
                            timeSize = eventSize;
                            locationColor = eventColor;
                            locationSize = eventSize;
                        }
                    } else {
                        if (singleLineEvent) {
                            eventItem.setInt(R.id.title, "setLines", 1);
                        }
                        eventColor = prefs.getInt("text_" + widgetId,
                                Fragment_Appearance.DEFAULT_EVENT_COLOR);
                        eventSize = prefs.getFloat("textsize_" + widgetId,
                                Fragment_Appearance.DEFAULT_EVENT_SIZE);
                    }

                    eventItem.setTextColor(R.id.title, eventColor);
                    eventItem.setFloat(R.id.title, "setTextSize", eventSize);
                    eventItem.setTextViewText(R.id.title, cs);
                    String timeText = null;

                    if (!e.allDay) {
                        if (!e.multiDay || e.multiDayIsOriginal) {
                            try {
                                timeText = new SimpleDateFormat(
                                        prefs.getString("timeformat_" + widgetId,
                                                Fragment_Appearance.getDefaultTimeFormat(context)))
                                        .format(e.date) + " ";
                            } catch (Exception ex) {
                                timeText = new SimpleDateFormat(
                                        Fragment_Appearance.getDefaultTimeFormat(context))
                                        .format(e.date) + " ";
                            }
                            if (e.multiDay && !alldayEndFormat.isEmpty()) {
                                try {
                                    timeText += "» " +
                                            new SimpleDateFormat(alldayEndFormat).format(e.end) +
                                            " ";
                                } catch (Exception ex) {
                                    timeText += "» " + new SimpleDateFormat(
                                            Fragment_Settings.DEFAULT_ALLDAY_END_FORMAT)
                                            .format(e.end) + " ";
                                }
                            }
                            if (e.end > 0 && prefs.getBoolean("endTimes_" + widgetId, false) &&
                                    (!e.multiDay || !alldayEndFormat.isEmpty())) {
                                try {
                                    timeText += "- " + new SimpleDateFormat(
                                            prefs.getString("timeformat_" + widgetId,
                                                    Fragment_Appearance
                                                            .getDefaultTimeFormat(context)))
                                            .format(e.end) + " ";
                                } catch (Exception ex) {
                                    timeText += "- " + new SimpleDateFormat(
                                            Fragment_Appearance.getDefaultTimeFormat(context))
                                            .format(e.end) + " ";
                                }
                            }
                        } else if (e.multiDay && e.end < day.date + 24 * 60 * 60 * 1000) {
                            try {
                                timeText = "» " + new SimpleDateFormat(
                                        prefs.getString("timeformat_" + widgetId,
                                                Fragment_Appearance.getDefaultTimeFormat(context)))
                                        .format(e.end) + " ";
                            } catch (Exception ex) {
                                timeText = "» " + new SimpleDateFormat(
                                        Fragment_Appearance.getDefaultTimeFormat(context))
                                        .format(e.end) + " ";
                            }
                        }
                    } else if (e.multiDay && !alldayEndFormat.isEmpty()) {
                        try {
                            timeText = "» " + new SimpleDateFormat(alldayEndFormat).format(e.end) +
                                    " ";
                        } catch (Exception ex) {
                            timeText = "» " + new SimpleDateFormat(
                                    Fragment_Settings.DEFAULT_ALLDAY_END_FORMAT).format(e.end) +
                                    " ";
                        }
                    }

                    if (timeText != null) {
                        cs = timeText;
                        if (prefs.getBoolean("timebold_" + widgetId, false)) {
                            SpannableString s = new SpannableString(cs);
                            s.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
                            cs = s;
                        }

                        eventItem.setTextViewText(R.id.time, cs);
                        eventItem.setFloat(R.id.time, "setTextSize", timeSize);
                        eventItem.setTextColor(R.id.time, timeColor);
                    }

                    if (e.location != null && prefs.getBoolean("showLocation_" + widgetId, true)) {

                        cs = e.location;
                        if (prefs.getBoolean("locationbold_" + widgetId, false)) {
                            SpannableString s = new SpannableString(cs);
                            s.setSpan(new StyleSpan(Typeface.BOLD), 0, cs.length(), 0);
                            cs = s;
                        }

                        if (singleLineLocation) {
                            eventItem.setInt(R.id.eventlocation, "setLines", 1);
                        }

                        eventItem.setTextViewText(R.id.eventlocation, cs);
                        eventItem.setTextColor(R.id.eventlocation, locationColor);
                        eventItem.setFloat(R.id.eventlocation, "setTextSize", locationSize);
                        eventItem.setViewVisibility(R.id.eventlocation, View.VISIBLE);
                    }

                    dayItem.addView(R.id.events, eventItem);
                }
                if (day.events.size() > 1 || !openEvent) {
                    dayItem.setOnClickFillInIntent(R.id.dayitem,
                            new Intent().putExtra("beginTime", day.date));
                } else {
                    Event e = day.events.get(0);
                    Intent i = new Intent().putExtra("eventid", e.id);
                    if (!e.allDay) {
                        i.putExtra("beginTime", e.date).putExtra("endTime", e.end);
                    }
                    dayItem.setOnClickFillInIntent(R.id.dayitem, i);
                }
                views.add(dayItem);
            }
            if (BuildConfig.DEBUG) Logger.log("events: " + views.size());
        }

        @Override
        public int getCount() {
            return views == null ? 0 : views.size();
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
            return 2;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDestroy() {
            if (BuildConfig.DEBUG) Logger.log("WidgetService::onDestroy " + widgetId);
        }

    }

}
