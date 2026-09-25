package de.j4velin.calendarWidget;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

class Parser {

    private final static int[] lookAheadMultiplier = new int[]{86400, 604800, 2678400, 31536000};

    private Parser() {
    }

    @SuppressLint("InlinedApi")
    static List<Day> parse(final Context c, int widgetId) {
        if (BuildConfig.DEBUG) Logger.log("Parser::parse " + widgetId);
        if (c == null) return null;
        ArrayList<Day> daysList = null;
        final SharedPreferences prefs =
                c.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        final String cals = prefs.getString("cals_" + widgetId, null);
        if (cals != null) {

            long maxTime =
                    lookAheadMultiplier[prefs.getInt("lookaheadUnit_" + widgetId, 1)];
            maxTime *= prefs.getInt("lookaheadtime_" + widgetId, 4) * 1000;

            boolean showMultiDayAllDayOnEveryDay =
                    prefs.getString("allday_endformat_" + widgetId, "").length() <= 0;
            boolean skipPassed = prefs.getBoolean("skipPassed_" + widgetId, false);

            final long todayMs = getToday();
            maxTime += todayMs;
            final Cursor cursor;

            long nextUpdate = Long.MAX_VALUE;

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();

            TimeZone tz = TimeZone.getDefault();
            long tzNow = Math.min(Math.min(todayMs, todayMs - tz.getOffset(todayMs)),
                    todayMs + tz.getOffset(todayMs));
            long tzMax = Math.max(Math.max(maxTime, maxTime - tz.getOffset(maxTime)),
                    maxTime + tz.getOffset(maxTime));

            ContentUris.appendId(builder, tzNow);
            ContentUris.appendId(builder, tzMax);

            if (BuildConfig.DEBUG) Logger.log(new Date(todayMs).toLocaleString() + " - " +
                    new Date(maxTime).toLocaleString() + " -> -tz: " +
                    new Date(tzNow).toLocaleString() + " - " + new Date(tzMax).toLocaleString());

            /*
            ContentUris.appendId(builder, todayMs);
            ContentUris.appendId(builder, maxTime);
            */

            cursor = c.getContentResolver().query(builder.build(),
                    new String[]{CalendarContract.Events.TITLE, CalendarContract.Instances.BEGIN,
                            CalendarContract.Events.EVENT_LOCATION, CalendarContract.Events.ALL_DAY,
                            CalendarContract.Instances.END, CalendarContract.Events.EVENT_COLOR,
                            CalendarContract.Events.SELF_ATTENDEE_STATUS,
                            CalendarContract.Instances.EVENT_ID,
                            CalendarContract.Events.CALENDAR_COLOR},
                    CalendarContract.Events.CALENDAR_ID + " IN (" + cals + ") AND " +
                            CalendarContract.Events.TITLE + " NOT NULL AND " +
                            CalendarContract.Events.DELETED + " != 1", null,
                    CalendarContract.Instances.BEGIN + " ASC");


            if (BuildConfig.DEBUG) Logger.log(
                    cursor.getCount() + " events found between " + tzNow + " and " + tzMax +
                            " (localtime: " + new Date(tzNow).toLocaleString() + " - " +
                            new Date(tzMax).toLocaleString());
            /*
            if (BuildConfig.DEBUG) Logger.log(
                    cursor.getCount() + " events found between " + todayMs + " and " + maxTime +
                            " (localtime: " + new Date(todayMs).toLocaleString() + " - " +
                            new Date(maxTime).toLocaleString());
             */

            Calendar cal = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            if (cursor != null && cursor.getCount() > 0) {
                if (BuildConfig.DEBUG) Logger.log(cursor);

                cursor.moveToFirst();

                long eventDate;
                long endDate;
                boolean allday;
                List<Event> events = new ArrayList<Event>(cursor.getCount());
                Event e;
                do {
                    eventDate = cursor.getLong(1);
                    allday = 1 == cursor.getInt(3);
                    if (CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED == cursor.getInt(6)) {
                        if (BuildConfig.DEBUG) Logger.log(
                                "Skipping " + cursor.getString(0) + " because status=declined");
                        continue;
                    }
                    //if (allday) eventDate -= tz.getOffset(eventDate);
                    if (eventDate > maxTime) {
                        if (BuildConfig.DEBUG) Logger.log(
                                "Skipping " + cursor.getString(0) + " because " + eventDate +
                                        " > " + maxTime);
                        continue;
                    }
                    try {
                        endDate = cursor.getLong(4);
                        if (endDate > System.currentTimeMillis())
                            nextUpdate = Math.min(nextUpdate, endDate);
                        //if (allday) endDate -= tz.getOffset(endDate);
                    } catch (NumberFormatException nfe) {
                        endDate = 0;
                    }
                    // skip passed events?
                    if (endDate > 0 && endDate < System.currentTimeMillis() && skipPassed) {
                        if (BuildConfig.DEBUG) Logger.log(
                                "Skipping " + cursor.getString(0) + " because already passed");
                        continue;
                    }
                    cal.setTimeInMillis(eventDate);
                    cal2.setTimeInMillis(endDate);
                    e = new Event(cursor.getLong(7), eventDate, cursor.getString(0),
                            cursor.getString(2), allday, endDate, cursor.getInt(5),
                            cursor.getInt(8), endDate > 0 &&
                            ((cal.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) &&
                                    !allday) ||
                                    (allday && endDate - eventDate > 24 * 60 * 60 * 1000)), true);
                    if (!showMultiDayAllDayOnEveryDay || eventDate > System.currentTimeMillis() ||
                            ((!skipPassed || endDate > System.currentTimeMillis()) &&
                                    cal.get(Calendar.DAY_OF_YEAR) ==
                                            today.get(Calendar.DAY_OF_YEAR))) {
                        // if event start < today
                        if (cal.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR) &&
                                cal.get(Calendar.YEAR) <= today.get(Calendar.YEAR)) {
                            // handle like all day event
                            cal.set(Calendar.DAY_OF_YEAR, today.get(Calendar.DAY_OF_YEAR));
                            events.add(new Event(cursor.getLong(7), cal.getTimeInMillis(),
                                    cursor.getString(0), cursor.getString(2), true, endDate,
                                    cursor.getInt(5), cursor.getInt(8), endDate > 0 &&
                                    ((cal.get(Calendar.DAY_OF_YEAR) !=
                                            cal2.get(Calendar.DAY_OF_YEAR) && !allday) ||
                                            (allday && endDate - eventDate > 24 * 60 * 60 * 1000)),
                                    true));
                        } else {
                            if (BuildConfig.DEBUG) Logger.log("Adding " + e.title);
                            events.add(e);
                        }
                    }
                    if (showMultiDayAllDayOnEveryDay && endDate > 0 && e.multiDay) {
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                        while (cal.getTimeInMillis() < Math.min(endDate, maxTime)) {
                            // if current date >= today
                            if (cal.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
                                    (cal.get(Calendar.DAY_OF_YEAR) >=
                                            today.get(Calendar.DAY_OF_YEAR) &&
                                            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR))) {
                                e = new Event(cursor.getLong(7), cal.getTimeInMillis(),
                                        cursor.getString(0), cursor.getString(2), allday, endDate,
                                        cursor.getInt(5), cursor.getInt(8), true, false);
                                events.add(e);
                            }
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                        }
                    }
                } while (cursor.moveToNext());

                final long current = System.currentTimeMillis();
                Collections.sort(events, new Comparator<Event>() {
                    @Override
                    public int compare(Event e1, Event e2) {
                        // force passed event to the beginning by making their starttime artificially smaller
                        long time1 = e1.end > 0 && e1.end < current ? e1.date / 2 : e1.date;
                        long time2 = e2.end > 0 && e2.end < current ? e2.date / 2 : e2.date;
                        return Long.compare(time1, time2);
                    }
                });

                daysList = new ArrayList<>((int) ((maxTime - todayMs) / 86400000));
                Day currentDay = null;
                cal.setTimeInMillis(System.currentTimeMillis());
                boolean stillOnSameDay = false;

                Event cur;
                for (int i = 0; i < events.size(); i++) {
                    cur = events.get(i);
                    if (currentDay != null) {
                        cal2.setTime(new Date(cur.date));
                        stillOnSameDay =
                                cal.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                                        cal.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
                    }

                    if (!stillOnSameDay) {
                        currentDay = new Day(cur.date);
                        daysList.add(currentDay);
                        cal.setTime(new Date(cur.date));
                    }
                    currentDay.events.add(cur);
                }
            }

            if (cursor != null) {
                cursor.close();
            }

            if (nextUpdate > System.currentTimeMillis()) {
                Widget.setNextAlarm(prefs, nextUpdate, widgetId, c);
            }

        } else if (BuildConfig.DEBUG) Logger.log("cals == null");
        return daysList;
    }

    public static long getToday() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
