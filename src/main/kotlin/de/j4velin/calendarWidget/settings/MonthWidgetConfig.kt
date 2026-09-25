package de.j4velin.calendarWidget.settings

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.Backup
import de.j4velin.calendarWidget.data.MonthSettings

/** Configuration of the month widget */
class MonthWidgetConfig : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val viewModel: ConfigViewModel<MonthSettings> by viewModels {
        viewModelFactory {
            initializer {
                ConfigViewModel(
                    application, widgetId, Backup.MONTH, MonthSettings::load, MonthSettings::save
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = WidgetConfig.widgetIdFrom(intent)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        enableEdgeToEdge()
        setContent { AppTheme { MonthConfigScreen(viewModel) } }
    }

    override fun onPause() {
        super.onPause()
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) viewModel.save()
    }

    companion object {
        /** Intent to open the configuration of an existing widget */
        internal fun editIntent(context: Context, widgetId: Int): Intent =
            Intent(context, MonthWidgetConfig::class.java).putExtra("editId", widgetId)
    }
}

@Composable
private fun TextAndBackgroundColor(
    title: String,
    textColor: Int,
    onTextColorChange: (Int) -> Unit,
    backgroundColor: Int,
    onBackgroundColorChange: (Int) -> Unit,
) {
    SectionTitle(title)
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        ColorSetting(stringResource(R.string.text_color), textColor, onTextColorChange)
        ColorSetting(
            stringResource(R.string.background_color), backgroundColor, onBackgroundColorChange,
            withAlpha = true,
        )
    }
}

@Composable
internal fun MonthConfigScreen(viewModel: ConfigViewModel<MonthSettings>) {
    // the calendar permission is only needed for the event indicators
    val requestPermission = rememberCalendarPermissionRequest(viewModel)
    ConfigScaffold(viewModel, requirePermission = false, requestPermission) { padding ->
        key(viewModel.version) {
            val settings = viewModel.settings
            val update = viewModel::update
            Box(modifier = Modifier.padding(padding)) {
                SettingsPage {
                    CheckboxRow(stringResource(R.string.week_start), settings.startOnMonday, {
                        update { copy(startOnMonday = it) }
                    })
                    TextAndBackgroundColor(
                        stringResource(R.string.curentdate),
                        settings.todayTextColor, { update { copy(todayTextColor = it) } },
                        settings.todayBackgroundColor, { update { copy(todayBackgroundColor = it) } },
                    )
                    TextAndBackgroundColor(
                        stringResource(R.string.current_month),
                        settings.monthTextColor, { update { copy(monthTextColor = it) } },
                        settings.monthBackgroundColor, { update { copy(monthBackgroundColor = it) } },
                    )
                    TextAndBackgroundColor(
                        stringResource(R.string.other_days),
                        settings.otherTextColor, { update { copy(otherTextColor = it) } },
                        settings.otherBackgroundColor, { update { copy(otherBackgroundColor = it) } },
                    )
                    SectionTitle(stringResource(R.string.day_label))
                    ColorSetting(stringResource(R.string.text_color), settings.dayLabelColor, {
                        update { copy(dayLabelColor = it) }
                    })
                    SectionTitle(stringResource(R.string.month_label))
                    ColorSetting(stringResource(R.string.text_color), settings.monthLabelColor, {
                        update { copy(monthLabelColor = it) }
                    })
                    Description(stringResource(R.string.month_label_config))
                    SectionTitle(stringResource(R.string.widget_background))
                    ColorSetting(
                        stringResource(R.string.background_color), settings.backgroundColor,
                        { update { copy(backgroundColor = it) } }, withAlpha = true,
                    )
                    SectionTitle(stringResource(R.string.prev_next_month_icons))
                    IconSettings(
                        iconColor = settings.iconColor,
                        iconAlpha = settings.iconAlpha,
                        previewIcon = R.drawable.ic_next,
                        onColorChange = { update { copy(iconColor = it) } },
                        onAlphaChange = { update { copy(iconAlpha = it) } },
                    )
                    SectionTitle(stringResource(R.string.indicators))
                    Description(stringResource(R.string.indicators_description))
                    if (viewModel.hasPermission) {
                        CalendarSelection(viewModel.calendars, settings.calendarIds) {
                            update { copy(calendarIds = it) }
                        }
                    } else {
                        PermissionRequired(requestPermission, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
