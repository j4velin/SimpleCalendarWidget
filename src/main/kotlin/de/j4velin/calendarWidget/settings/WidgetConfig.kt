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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.Backup
import de.j4velin.calendarWidget.data.ClickAction
import kotlinx.coroutines.launch

/** Configuration of the agenda widget */
class WidgetConfig : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val viewModel: ConfigViewModel<AgendaSettings> by viewModels {
        viewModelFactory {
            initializer {
                ConfigViewModel(
                    application, widgetId, Backup.AGENDA, AgendaSettings::load, AgendaSettings::save
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = widgetIdFrom(intent)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        enableEdgeToEdge()
        setContent { AppTheme { AgendaConfigScreen(viewModel) } }
    }

    override fun onPause() {
        super.onPause()
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) viewModel.save()
    }

    companion object {
        private const val EXTRA_EDIT_ID = "editId"

        /** Intent to open the configuration of an existing widget */
        internal fun editIntent(context: Context, widgetId: Int): Intent =
            Intent(context, WidgetConfig::class.java).putExtra(EXTRA_EDIT_ID, widgetId)

        internal fun widgetIdFrom(intent: Intent): Int = intent.getIntExtra(
            EXTRA_EDIT_ID, intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
    }
}

@Composable
internal fun AgendaConfigScreen(viewModel: ConfigViewModel<AgendaSettings>) {
    val requestPermission = rememberCalendarPermissionRequest(viewModel)
    ConfigScaffold(viewModel, requirePermission = true, requestPermission) { padding ->
        val tabs = listOf(R.string.events, R.string.appearance, R.string.settings)
        val pagerState = rememberPagerState { tabs.size }
        val scope = rememberCoroutineScope()
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(title)) },
                    )
                }
            }
            // reset all text field states when the settings are replaced by a backup
            key(viewModel.version) {
                val settings = viewModel.settings
                val update = viewModel::update
                HorizontalPager(state = pagerState, beyondViewportPageCount = 2) { page ->
                    when (page) {
                        0 -> EventsPage(settings, viewModel, update)
                        1 -> AppearancePage(settings, update)
                        else -> BehaviorPage(settings, update)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsPage(
    settings: AgendaSettings,
    viewModel: ConfigViewModel<AgendaSettings>,
    update: (AgendaSettings.() -> AgendaSettings) -> Unit,
) = SettingsPage {
    SectionTitle(stringResource(R.string.calendars))
    CalendarSelection(viewModel.calendars, settings.calendarIds) {
        update { copy(calendarIds = it) }
    }
    SectionTitle(stringResource(R.string.lookahead))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberField(
            label = "#",
            value = settings.lookAheadTime,
            onValueChange = { update { copy(lookAheadTime = it) } },
            modifier = Modifier.width(96.dp),
        )
        Dropdown(
            label = stringResource(R.string.lookahead),
            options = stringArrayResource(R.array.lookAheadTimes).toList(),
            selectedIndex = settings.lookAheadUnit,
            onSelect = { update { copy(lookAheadUnit = it) } },
        )
    }
    CheckboxRow(stringResource(R.string.calendarcolor), settings.showCalendarColors, {
        update { copy(showCalendarColors = it) }
    })
}

@Composable
private fun AppearancePage(
    settings: AgendaSettings,
    update: (AgendaSettings.() -> AgendaSettings) -> Unit,
) = SettingsPage {
    SectionTitle(stringResource(R.string.curentdate))
    CheckboxRow(stringResource(R.string.showcurrentdate), settings.showCurrentDate, {
        update { copy(showCurrentDate = it) }
    })
    if (settings.showCurrentDate) {
        ColorAndSize(
            settings.currentDateColor, { update { copy(currentDateColor = it) } },
            settings.currentDateSize, { update { copy(currentDateSize = it) } },
        )
        CheckboxRow(stringResource(R.string.bold), settings.currentDateBold, {
            update { copy(currentDateBold = it) }
        })
        DateFormatField(
            stringResource(R.string.dateformat), settings.currentDateFormat, DATE_FORMAT_PRESETS,
            { update { copy(currentDateFormat = it) } },
        )
    }

    SectionTitle(stringResource(R.string.eventdate))
    ColorAndSize(
        settings.dateColor, { update { copy(dateColor = it) } },
        settings.dateSize, { update { copy(dateSize = it) } },
    )
    CheckboxRow(stringResource(R.string.bold), settings.dateBold, { update { copy(dateBold = it) } })
    CheckboxRow(stringResource(R.string.light), settings.dateLight, { update { copy(dateLight = it) } })
    DateFormatField(
        stringResource(R.string.dateformat), settings.dateFormat, DATE_FORMAT_PRESETS,
        { update { copy(dateFormat = it) } },
    )

    SectionTitle(stringResource(R.string.eventtime))
    ColorAndSize(
        settings.timeColor, { update { copy(timeColor = it) } },
        settings.timeSize, { update { copy(timeSize = it) } },
    )
    CheckboxRow(stringResource(R.string.bold), settings.timeBold, { update { copy(timeBold = it) } })
    DateFormatField(
        stringResource(R.string.timeformat), settings.timeFormat, TIME_FORMAT_PRESETS,
        { update { copy(timeFormat = it) } },
    )
    CheckboxRow(stringResource(R.string.showendtimes), settings.showEndTimes, {
        update { copy(showEndTimes = it) }
    })

    SectionTitle(stringResource(R.string.eventname))
    ColorAndSize(
        settings.eventColor, { update { copy(eventColor = it) } },
        settings.eventSize, { update { copy(eventSize = it) } },
    )
    CheckboxRow(stringResource(R.string.bold), settings.eventBold, { update { copy(eventBold = it) } })
    CheckboxRow(stringResource(R.string.light), settings.eventLight, { update { copy(eventLight = it) } })
    CheckboxRow(stringResource(R.string.single_line), settings.singleLineEvent, {
        update { copy(singleLineEvent = it) }
    })

    SectionTitle(stringResource(R.string.eventlocation))
    CheckboxRow(stringResource(R.string.showlocation), settings.showLocation, {
        update { copy(showLocation = it) }
    })
    if (settings.showLocation) {
        ColorAndSize(
            settings.locationColor, { update { copy(locationColor = it) } },
            settings.locationSize, { update { copy(locationSize = it) } },
        )
        CheckboxRow(stringResource(R.string.bold), settings.locationBold, {
            update { copy(locationBold = it) }
        })
        CheckboxRow(stringResource(R.string.single_line), settings.singleLineLocation, {
            update { copy(singleLineLocation = it) }
        })
    }

    SectionTitle(stringResource(R.string.todayevent))
    ColorAndSize(
        settings.todayEventColor, { update { copy(todayEventColor = it) } },
        settings.todayEventSize, { update { copy(todayEventSize = it) } },
    )
    CheckboxRow(stringResource(R.string.single_line), settings.singleLineToday, {
        update { copy(singleLineToday = it) }
    })

    SectionTitle(stringResource(R.string.todaypassedevent))
    CheckboxRow(stringResource(R.string.skippedpassed), settings.skipPassed, {
        update { copy(skipPassed = it) }
    })
    if (!settings.skipPassed) {
        ColorAndSize(
            settings.todayPassedEventColor, { update { copy(todayPassedEventColor = it) } },
            settings.todayPassedEventSize, { update { copy(todayPassedEventSize = it) } },
        )
        CheckboxRow(stringResource(R.string.single_line), settings.singleLineTodayPassed, {
            update { copy(singleLineTodayPassed = it) }
        })
    }

    SectionTitle(stringResource(R.string.time_location_for_todays_events))
    RadioRow(stringResource(R.string.same_as_event), settings.timeLocationTodaySameAsEvent, {
        update { copy(timeLocationTodaySameAsEvent = true) }
    })
    RadioRow(stringResource(R.string.same_as_on_other_days), !settings.timeLocationTodaySameAsEvent, {
        update { copy(timeLocationTodaySameAsEvent = false) }
    })

    SectionTitle(stringResource(R.string.background))
    ColorSetting(
        stringResource(R.string.color), settings.backgroundColor,
        { update { copy(backgroundColor = it) } }, withAlpha = true,
    )
}

@Composable
private fun BehaviorPage(
    settings: AgendaSettings,
    update: (AgendaSettings.() -> AgendaSettings) -> Unit,
) = SettingsPage {
    val context = LocalContext.current
    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    val otherAppLabel = remember(settings.otherApp) { appLabel(context, settings.otherApp) }

    CheckboxRow(stringResource(R.string.todaytomorrow), settings.todayTomorrowText, {
        update { copy(todayTomorrowText = it) }
    })

    SectionTitle(stringResource(R.string.multidayevents))
    Description(stringResource(R.string.multiday))
    RadioRow(stringResource(R.string.oneveryday), settings.multiDayOnEveryDay, {
        update { copy(multiDayOnEveryDay = true) }
    })
    RadioRow(stringResource(R.string.onlyonfirst), !settings.multiDayOnEveryDay, {
        update { copy(multiDayOnEveryDay = false) }
    })
    if (!settings.multiDayOnEveryDay) {
        DateFormatField(
            stringResource(R.string.dateformat), settings.alldayEndFormat,
            SHORT_DATE_FORMAT_PRESETS, { update { copy(alldayEndFormat = it) } },
            modifier = Modifier.padding(start = 48.dp),
        )
    }

    SectionTitle(stringResource(R.string.calendarapp))
    Description(stringResource(R.string.calendarappsummary))
    RadioRow(stringResource(R.string.defaultapp), settings.clickAction == ClickAction.DEFAULT_APP, {
        update { copy(clickAction = ClickAction.DEFAULT_APP) }
    })
    RadioRow(
        text = stringResource(R.string.otherapp),
        selected = settings.clickAction == ClickAction.OTHER_APP,
        onClick = { showAppPicker = true },
        supportingText = otherAppLabel.takeIf { settings.clickAction == ClickAction.OTHER_APP },
    )
    RadioRow(stringResource(R.string.update_on_click), settings.clickAction == ClickAction.UPDATE, {
        update { copy(clickAction = ClickAction.UPDATE) }
    })
    if (settings.clickAction != ClickAction.UPDATE) {
        CheckboxRow(stringResource(R.string.open_event), settings.openSingleEvent, {
            update { copy(openSingleEvent = it) }
        })
    }

    SectionTitle(stringResource(R.string.icons))
    IconSettings(
        iconColor = settings.iconColor,
        iconAlpha = settings.iconAlpha,
        previewIcon = R.drawable.ic_settings,
        onColorChange = { update { copy(iconColor = it) } },
        onAlphaChange = { update { copy(iconAlpha = it) } },
    )
    Description(stringResource(R.string.iconposition))

    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = {
                update { copy(clickAction = ClickAction.OTHER_APP, otherApp = it.intentUri) }
                showAppPicker = false
            },
        )
    }
}
