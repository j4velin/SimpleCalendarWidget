package de.j4velin.calendarWidget.settings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.j4velin.calendarWidget.data.widgetPrefs

/**
 * Common behavior of the widget configuration activities: changes are only saved with "Done".
 * Leaving the screen otherwise discards them, and cancels the placement of a new widget.
 */
abstract class ConfigActivity<S> : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isNewWidget = false

    internal abstract val backupType: String
    internal abstract fun load(context: Context, widgetId: Int): S
    internal abstract fun store(settings: S, prefs: SharedPreferences, widgetId: Int)
    internal abstract fun withCalendars(settings: S, calendarIds: Set<Long>): S

    @Composable
    internal abstract fun Screen(viewModel: ConfigViewModel<S>, onDone: () -> Unit, onCancel: () -> Unit)

    private val viewModel: ConfigViewModel<S> by viewModels {
        viewModelFactory {
            initializer {
                ConfigViewModel(
                    application, widgetId, backupType, ::load,
                    { prefs, id -> store(this, prefs, id) }, { withCalendars(this, it) },
                    isNewWidget,
                )
            }
        }
    }

    private val result
        get() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = widgetIdFrom(intent)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        // started by the launcher (not via the widget's settings icon) and never configured
        // (rendering the widget only stores the time of its next update)
        isNewWidget = !intent.hasExtra(EXTRA_EDIT_ID) && widgetPrefs().all.keys.none {
            it.endsWith("_$widgetId") && !it.startsWith("nextUpdate_")
        }
        // a new widget is only placed if the configuration is completed with "Done"
        setResult(RESULT_CANCELED, result)
        enableEdgeToEdge()
        setContent {
            AppTheme { Screen(viewModel, onDone = ::done, onCancel = ::finish) }
        }
    }

    private fun done() {
        viewModel.save()
        setResult(RESULT_OK, result)
        finish()
    }

    companion object {
        internal const val EXTRA_EDIT_ID = "editId"

        internal fun widgetIdFrom(intent: Intent): Int = intent.getIntExtra(
            EXTRA_EDIT_ID, intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
    }
}
