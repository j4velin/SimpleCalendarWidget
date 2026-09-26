package de.j4velin.calendarWidget.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.j4velin.calendarWidget.data.Backup
import de.j4velin.calendarWidget.data.CalendarInfo
import de.j4velin.calendarWidget.data.CalendarRepository
import de.j4velin.calendarWidget.data.widgetPrefs
import de.j4velin.calendarWidget.log
import de.j4velin.calendarWidget.widget.WidgetUpdates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Holds the settings of one widget while its configuration screen is shown.
 *
 * @param S the settings type
 */
internal class ConfigViewModel<S>(
    application: Application,
    val widgetId: Int,
    private val backupType: String,
    private val load: (Context, Int) -> S,
    private val store: S.(SharedPreferences, Int) -> Unit,
    private val withCalendars: S.(Set<Long>) -> S,
    /** a widget which is being placed right now, i.e. has not been configured before */
    val isNewWidget: Boolean,
) : AndroidViewModel(application) {

    /** new widgets show all visible calendars by default */
    private var preselectCalendars = isNewWidget

    private val repository = CalendarRepository(application)

    var settings by mutableStateOf(load(application, widgetId))
        private set

    /** the settings as they are stored */
    private var stored = settings

    /** whether leaving the screen now would discard anything */
    val hasUnsavedChanges: Boolean
        get() = isNewWidget || settings != stored

    /** the calendars on this device, null while loading */
    var calendars by mutableStateOf<List<CalendarInfo>?>(null)
        private set

    var hasPermission by mutableStateOf(repository.hasPermission())
        private set

    /** changes whenever the settings are replaced, e.g. by restoring a backup */
    var version by mutableIntStateOf(0)
        private set

    init {
        if (hasPermission) loadCalendars()
    }

    fun update(transform: S.() -> S) {
        settings = settings.transform()
    }

    fun onPermissionResult(granted: Boolean) {
        hasPermission = granted
        if (granted) loadCalendars()
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.calendars() }
            if (preselectCalendars) {
                preselectCalendars = false
                settings = settings.withCalendars(loaded.filter { it.visible }.map { it.id }.toSet())
            }
            calendars = loaded
        }
    }

    /** Saves the settings and updates the widget */
    fun save() {
        val app = getApplication<Application>()
        settings.store(app.widgetPrefs(), widgetId)
        stored = settings
        log("settings saved for widget $widgetId")
        WidgetUpdates.refreshAsync(app, widgetId)
    }

    fun backup(): File? {
        save()
        return Backup.save(getApplication(), widgetId, backupType)
    }

    fun restore(): Backup.RestoreResult {
        val app = getApplication<Application>()
        val result = Backup.restore(app, widgetId, backupType)
        if (result == Backup.RestoreResult.RESTORED) {
            // the restored settings are stored already
            settings = load(app, widgetId)
            stored = settings
            version++
            WidgetUpdates.refreshAsync(app, widgetId)
        }
        return result
    }
}
