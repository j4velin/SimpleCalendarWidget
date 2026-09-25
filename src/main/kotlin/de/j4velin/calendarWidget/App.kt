package de.j4velin.calendarWidget

import android.app.Application
import androidx.work.Configuration
import de.j4velin.calendarWidget.widget.WidgetUpdates

class App : Application(), Configuration.Provider {

    // Glance uses WorkManager internally, keep its job ids away from our own JobScheduler job
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setJobSchedulerJobIdRange(WidgetUpdates.JOB_ID_MAX + 1, Int.MAX_VALUE)
            .build()
}
