package de.j4velin.calendarWidget.settings;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.provider.CalendarContract;

import de.j4velin.calendarWidget.BuildConfig;
import de.j4velin.calendarWidget.Logger;
import de.j4velin.calendarWidget.UpdaterJob;

public class API24Wrapper {
    private final static int JOB_UPDATE = 1;

    @TargetApi(Build.VERSION_CODES.N)
    public static void scheduleCalenderUpdateJob(final Context context) {
        if (BuildConfig.DEBUG) Logger.log("scheduling updateJob");
        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(
                new JobInfo.Builder(JOB_UPDATE, new ComponentName(context, UpdaterJob.class))
                        .addTriggerContentUri(new JobInfo.TriggerContentUri(
                                CalendarContract.Instances.CONTENT_URI,
                                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS)).build());
        if (result != JobScheduler.RESULT_SUCCESS && BuildConfig.DEBUG)
            Logger.log("error scheduling job");
    }
}
