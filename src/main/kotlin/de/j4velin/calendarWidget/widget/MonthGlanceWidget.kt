package de.j4velin.calendarWidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.j4velin.calendarWidget.CalendarActionActivity
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.CalendarRepository
import de.j4velin.calendarWidget.data.DAY_MS
import de.j4velin.calendarWidget.data.Defaults
import de.j4velin.calendarWidget.data.MONTH_GRID_WEEKS
import de.j4velin.calendarWidget.data.MonthGrid
import de.j4velin.calendarWidget.data.MonthSettings
import de.j4velin.calendarWidget.data.countEventsPerDay
import de.j4velin.calendarWidget.data.startMillis
import de.j4velin.calendarWidget.settings.MonthWidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

internal data class MonthData(
    val widgetId: Int,
    val settings: MonthSettings,
    val grid: MonthGrid,
    val today: LocalDate,
    val label: String,
    val weekDayLabels: List<String>,
    val eventCounts: Map<LocalDate, Int>,
    val zone: ZoneId,
)

internal class MonthGlanceWidget : GlanceAppWidget() {

    // the layout depends on the size (header of small widgets)
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = previewMonth()
        provideContent { MonthContent(data) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialRefresh = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[
            WidgetUpdates.RefreshKey]
        val initial = withContext(Dispatchers.IO) { loadMonth(context, widgetId) }
        provideContent {
            val refresh = currentState<Preferences>()[WidgetUpdates.RefreshKey]
            val data by produceState(initial, refresh) {
                if (refresh != initialRefresh) {
                    value = withContext(Dispatchers.IO) { loadMonth(context, widgetId) }
                }
            }
            MonthContent(data)
        }
    }
}

/** Shows the previous or next month */
internal class MonthNavigationAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DeltaKey] ?: return
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        MonthSettings.changeMonthOffset(context, widgetId, delta)
        WidgetUpdates.refresh(context, MonthGlanceWidget(), glanceId)
    }

    companion object {
        val DeltaKey = ActionParameters.Key<Int>("delta")
    }
}

internal fun loadMonth(context: Context, widgetId: Int): MonthData {
    val zone = ZoneId.systemDefault()
    WidgetUpdates.scheduleUpdate(context, widgetId, WidgetUpdates.nextMidnight(zone))
    return monthData(widgetId, MonthSettings.load(context, widgetId), zone) { grid ->
        val instances = CalendarRepository(context).instances(
            grid.calendarIds,
            grid.grid.firstDay.startMillis(zone) - DAY_MS,
            grid.grid.lastDay.plusDays(1).startMillis(zone) + DAY_MS,
            zone,
        )
        countEventsPerDay(instances, grid.grid, zone)
    }
}

private class GridRequest(val grid: MonthGrid, val calendarIds: Set<Long>)

private fun monthData(
    widgetId: Int,
    settings: MonthSettings,
    zone: ZoneId,
    eventCounts: (GridRequest) -> Map<LocalDate, Int>,
): MonthData {
    val locale = Locale.getDefault()
    val today = LocalDate.now(zone)
    val grid = MonthGrid.of(today, settings.monthOffset, settings.startOnMonday)
    val labelFormat = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale)
    return MonthData(
        widgetId = widgetId,
        settings = settings,
        grid = grid,
        today = today,
        label = labelFormat.format(grid.month.atDay(1).startMillis(zone)),
        weekDayLabels = grid.weekDays.map {
            // some locales abbreviate with a trailing dot, e.g. "Mo." in German
            it.getDisplayName(DateTextStyle.SHORT, locale).removeSuffix(".").uppercase(locale)
        },
        eventCounts = eventCounts(GridRequest(grid, settings.calendarIds)),
        zone = zone,
    )
}

/** Sample content for the widget picker, with some event indicators around today */
private fun previewMonth(): MonthData = monthData(
    AppWidgetManager.INVALID_APPWIDGET_ID,
    // the default background is transparent, which is hard to see in the widget picker
    MonthSettings(backgroundColor = Defaults.BG_COLOR),
    ZoneId.systemDefault(),
) { request ->
    val today = LocalDate.now()
    mapOf(today to 2, today.plusDays(2) to 1, today.plusDays(5) to 3, today.minusDays(3) to 1)
        .filterKeys { it.month == request.grid.month.month }
}

private const val MAX_EVENT_DOTS = 5

@Composable
internal fun MonthContent(data: MonthData) {
    val context = LocalContext.current
    val settings = data.settings
    val tint = ColorFilter.tint(ColorProvider(Color(settings.iconColor.color)))
    val iconAlpha = settings.iconAlpha / 255f
    val small = isSmallerThan(SMALL_MONTH)
    val iconSize = if (small) 36.dp else 48.dp

    Column(
        modifier = GlanceModifier.fillMaxSize().widgetBackground(settings.backgroundColor)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_prev),
                contentDescription = context.getString(R.string.previous_month),
                alpha = iconAlpha,
                colorFilter = tint,
                modifier = GlanceModifier.size(iconSize).clickable(
                    actionRunCallback<MonthNavigationAction>(
                        actionParametersOf(MonthNavigationAction.DeltaKey to -1)
                    )
                ),
            )
            Text(
                text = data.label,
                style = TextStyle(
                    color = ColorProvider(Color(settings.monthLabelColor)),
                    fontSize = if (small) 16.sp else 20.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = GlanceModifier.defaultWeight().clickable(
                    actionStartActivity(MonthWidgetConfig.editIntent(context, data.widgetId))
                ),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_next),
                contentDescription = context.getString(R.string.next_month),
                alpha = iconAlpha,
                colorFilter = tint,
                modifier = GlanceModifier.size(iconSize).clickable(
                    actionRunCallback<MonthNavigationAction>(
                        actionParametersOf(MonthNavigationAction.DeltaKey to 1)
                    )
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(if (small) 2.dp else 10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            data.weekDayLabels.forEach { label ->
                Text(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(settings.dayLabelColor)),
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight().padding(vertical = 4.dp),
                )
            }
        }
        for (week in 0 until MONTH_GRID_WEEKS) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                for (weekDay in 0 until 7) {
                    DayCell(data, data.grid.days[week * 7 + weekDay], GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun DayCell(data: MonthData, date: LocalDate, modifier: GlanceModifier) {
    val context = LocalContext.current
    val settings = data.settings
    val (textColor, backgroundColor) = when {
        date == data.today -> settings.todayTextColor to settings.todayBackgroundColor
        date.month == data.grid.month.month -> settings.monthTextColor to settings.monthBackgroundColor
        else -> settings.otherTextColor to settings.otherBackgroundColor
    }
    val click = if (settings.openCalendar) {
        actionStartActivity(
            CalendarActionActivity.openCalendar(context, data.widgetId, date.startMillis(data.zone))
        )
    } else {
        actionRunCallback<RefreshAction>()
    }
    val style = TextStyle(color = ColorProvider(Color(textColor)), textAlign = TextAlign.Center)
    val events = data.eventCounts[date] ?: 0

    Box(
        modifier = modifier.fillMaxHeight().background(ColorProvider(Color(backgroundColor)))
            .clickable(click),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = date.dayOfMonth.toString(), style = style, maxLines = 1)
        if (events > 0) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Text(text = ".".repeat(minOf(events, MAX_EVENT_DOTS)), style = style, maxLines = 1)
            }
        }
    }
}
