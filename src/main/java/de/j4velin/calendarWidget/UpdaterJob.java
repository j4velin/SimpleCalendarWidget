package de.j4velin.calendarWidget;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import de.j4velin.calendarWidget.settings.API24Wrapper;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UpdaterJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (BuildConfig.DEBUG) Logger.log("-- starting updateJob");
        final AppWidgetManager awm = AppWidgetManager.getInstance(this);
        final SharedPreferences prefs =
                getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        for (int id : Widget.getAllWidgetIds(this)) {
            boolean isMonthWidget = prefs.contains("month_offset_" + id);
            if (isMonthWidget) {
                awm.updateAppWidget(id, MonthWidget.updateWidget(id, this));
                awm.notifyAppWidgetViewDataChanged(id, R.id.days);
            } else {
                awm.updateAppWidget(id, Widget.updateWidget(id, this));
                awm.notifyAppWidgetViewDataChanged(id, R.id.events);
            }
        }
        API24Wrapper.scheduleCalenderUpdateJob(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (BuildConfig.DEBUG) Logger.log("-- stopping updateJob");
        return false;
    }
}
