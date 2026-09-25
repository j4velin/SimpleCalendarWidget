package de.j4velin.calendarWidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Objects;

public class MonthReceiver extends BroadcastReceiver {

    public final static String NEXT = "next";
    public final static String PREVIOUS = "prev";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        int widgetId = intent.getIntExtra("widgetId", -1);

        if (BuildConfig.DEBUG) Logger.log("MonthReceiver " + widgetId);

        final SharedPreferences prefs =
                context.getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);

        prefs.edit().putInt("month_offset_" + widgetId,
                prefs.getInt("month_offset_" + widgetId, 0) +
                        (Objects.equals(intent.getAction(), NEXT) ? 1 : -1)).apply();

        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.days);
        appWidgetManager.updateAppWidget(widgetId, MonthWidget.updateWidget(widgetId, context));


    }
}
