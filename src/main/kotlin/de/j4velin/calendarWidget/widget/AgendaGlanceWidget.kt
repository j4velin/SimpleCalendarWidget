package de.j4velin.calendarWidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
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
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.j4velin.calendarWidget.CalendarActionActivity
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.CalendarRepository
import de.j4velin.calendarWidget.data.ClickAction
import de.j4velin.calendarWidget.data.DAY_MS
import de.j4velin.calendarWidget.data.Defaults
import de.j4velin.calendarWidget.data.buildAgenda
import de.j4velin.calendarWidget.data.startMillis
import de.j4velin.calendarWidget.data.toLocalDate
import de.j4velin.calendarWidget.settings.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class AgendaEventItem(
    val title: String,
    val location: String?,
    val time: String?,
    val color: Int,
    val passed: Boolean,
)

internal data class AgendaDayItem(
    val title: String,
    val isToday: Boolean,
    val events: List<AgendaEventItem>,
    /** intent to start when the day is clicked */
    val click: Intent,
)

internal data class AgendaData(
    val widgetId: Int,
    val settings: AgendaSettings,
    val hasPermission: Boolean,
    val header: String?,
    val days: List<AgendaDayItem>,
)

internal class AgendaGlanceWidget : GlanceAppWidget() {

    // the layout depends on the size (icons of small widgets)
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val data = previewAgenda(context)
        provideContent { AgendaContent(data) }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialRefresh = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[
            WidgetUpdates.RefreshKey]
        val initial = withContext(Dispatchers.IO) { loadAgenda(context, widgetId) }
        provideContent {
            val refresh = currentState<Preferences>()[WidgetUpdates.RefreshKey]
            val data by produceState(initial, refresh) {
                if (refresh != initialRefresh) {
                    value = withContext(Dispatchers.IO) { loadAgenda(context, widgetId) }
                }
            }
            AgendaContent(data)
        }
    }
}

/** Refreshes the widget, used as click action if the widget should not open an app */
internal class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetUpdates.refresh(context, GlanceAppWidgetManager(context).getAppWidgetId(glanceId))
    }
}

internal fun loadAgenda(context: Context, widgetId: Int): AgendaData {
    val settings = AgendaSettings.load(context, widgetId)
    val repository = CalendarRepository(context)
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val header = if (settings.showCurrentDate) {
        SafeDateFormat(settings.currentDateFormat, Defaults.todayDateFormat, Locale.getDefault())
            .format(now)
    } else null

    if (!repository.hasPermission()) {
        return AgendaData(widgetId, settings, hasPermission = false, header, emptyList())
    }

    val today = now.toLocalDate(zone)
    val todayStart = today.startMillis(zone)
    val lookAheadEnd = settings.lookAheadEnd(todayStart)
    val agenda = buildAgenda(
        instances = repository.instances(
            settings.calendarIds, todayStart - DAY_MS, lookAheadEnd + DAY_MS, zone
        ),
        now = now,
        zone = zone,
        lookAheadEnd = lookAheadEnd,
        multiDayOnEveryDay = settings.multiDayOnEveryDay,
        skipPassed = settings.skipPassed,
    )
    WidgetUpdates.scheduleUpdate(
        context, widgetId, minOf(WidgetUpdates.nextMidnight(zone), agenda.nextChange ?: Long.MAX_VALUE)
    )

    val formatter = AgendaFormatter(
        settings,
        defaultTimeFormat = Defaults.timeFormat(context),
        defaultDateFormat = Defaults.dateFormat,
        defaultAlldayEndFormat = Defaults.alldayEndFormat,
    )
    val todayText = context.getString(R.string.today)
    val tomorrowText = context.getString(R.string.tomorrow)
    val days = agenda.days.map { day ->
        val relativeDay = ChronoUnit.DAYS.between(today, day.date.toLocalDate(zone))
        val single = day.events.singleOrNull()
        val click = if (settings.openSingleEvent && single != null) {
            CalendarActionActivity.openEvent(
                context, widgetId, single.id,
                begin = if (single.allDay) null else single.date,
                end = if (single.allDay) null else single.end,
            )
        } else {
            CalendarActionActivity.openCalendar(context, widgetId, day.date)
        }
        AgendaDayItem(
            title = formatter.dayTitle(day, relativeDay, todayText, tomorrowText),
            isToday = relativeDay == 0L,
            events = day.events.map { event ->
                AgendaEventItem(
                    title = event.title,
                    location = event.location?.takeIf { settings.showLocation },
                    time = formatter.timeText(event, day),
                    color = event.color,
                    passed = event.isPassed(now),
                )
            },
            click = click,
        )
    }
    return AgendaData(widgetId, settings, hasPermission = true, header, days)
}

private const val LIGHT_FONT = "sans-serif-light"

private fun textStyle(color: Int, size: Float, bold: Boolean, light: Boolean = false) = TextStyle(
    color = ColorProvider(Color(color)),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = if (light) FontFamily(LIGHT_FONT) else null,
)

@Composable
internal fun AgendaContent(data: AgendaData) {
    val context = LocalContext.current
    val settings = data.settings
    val openCalendar = actionStartActivity(CalendarActionActivity.openCalendar(context, data.widgetId))
    val dayClick: (AgendaDayItem) -> Action = {
        if (settings.clickAction == ClickAction.UPDATE) actionRunCallback<RefreshAction>()
        else actionStartActivity(it.click)
    }

    Column(
        modifier = GlanceModifier.fillMaxSize().widgetBackground(settings.backgroundColor)
    ) {
        if (data.header != null) {
            Text(
                text = data.header,
                style = textStyle(
                    settings.currentDateColor, settings.currentDateSize,
                    settings.currentDateBold, settings.dateLight
                ).copy(textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 5.dp).clickable(openCalendar),
            )
        }
        Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            when {
                !data.hasPermission -> Text(
                    text = context.getString(R.string.no_permission_widget),
                    style = textStyle(settings.eventColor, settings.eventSize, bold = false),
                    modifier = GlanceModifier.fillMaxSize().padding(5.dp)
                        .clickable(actionStartActivity(WidgetConfig.editIntent(context, data.widgetId))),
                )

                data.days.isEmpty() -> Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(openCalendar)
                ) {}

                else -> LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                        .padding(start = 5.dp, top = 5.dp, end = 10.dp, bottom = 5.dp)
                ) {
                    // one row per day title and per event: Glance drops all children of a
                    // Column after the 10th, so a day's events can't be nested in one item
                    data.days.forEach { day ->
                        val click = GlanceModifier.clickable(dayClick(day))
                        item { DayTitle(day, settings, click) }
                        day.events.forEachIndexed { index, event ->
                            item {
                                val last = index == day.events.lastIndex
                                EventItem(
                                    event, day.isToday, settings,
                                    click.padding(bottom = if (last) 5.dp else 0.dp),
                                )
                            }
                        }
                    }
                }
            }
            WidgetIcons(settings, data.widgetId)
        }
    }
}

@Composable
private fun WidgetIcons(settings: AgendaSettings, widgetId: Int) {
    val context = LocalContext.current
    val tint = ColorFilter.tint(ColorProvider(Color(settings.iconColor.color)))
    val alpha = settings.iconAlpha / 255f
    // small widgets only show a smaller settings icon, so the icons don't cover the events
    val small = isSmallerThan(SMALL_AGENDA)
    if (!small) Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = context.getString(R.string.add_event),
            alpha = alpha,
            colorFilter = tint,
            modifier = GlanceModifier.size(50.dp).padding(15.dp)
                .clickable(actionStartActivity(CalendarActionActivity.addEvent(context, widgetId))),
        )
    }
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Image(
            provider = ImageProvider(R.drawable.ic_settings),
            contentDescription = context.getString(R.string.edit_widget),
            alpha = alpha,
            colorFilter = tint,
            modifier = GlanceModifier.size(if (small) 36.dp else 50.dp)
                .padding(if (small) 10.dp else 15.dp)
                .clickable(actionStartActivity(WidgetConfig.editIntent(context, widgetId))),
        )
    }
}

@Composable
private fun DayTitle(day: AgendaDayItem, settings: AgendaSettings, modifier: GlanceModifier) {
    Text(
        text = day.title,
        style = textStyle(settings.dateColor, settings.dateSize, settings.dateBold, settings.dateLight),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun EventItem(
    event: AgendaEventItem, isToday: Boolean, settings: AgendaSettings, modifier: GlanceModifier,
) {
    val (color, size, singleLine) = when {
        !isToday -> Triple(settings.eventColor, settings.eventSize, settings.singleLineEvent)
        event.passed -> Triple(
            settings.todayPassedEventColor, settings.todayPassedEventSize, settings.singleLineTodayPassed
        )

        else -> Triple(settings.todayEventColor, settings.todayEventSize, settings.singleLineToday)
    }
    val sameAsEvent = isToday && settings.timeLocationTodaySameAsEvent
    val timeColor = if (sameAsEvent) color else settings.timeColor
    val timeSize = if (sameAsEvent) size else settings.timeSize
    val locationColor = if (sameAsEvent) color else settings.locationColor
    val locationSize = if (sameAsEvent) size else settings.locationSize

    Row(modifier = modifier.fillMaxWidth()) {
        if (settings.showCalendarColors) {
            Box(
                modifier = GlanceModifier.width(5.dp).fillMaxHeight()
                    .background(ColorProvider(Color(event.color)))
            ) {}
        }
        Spacer(modifier = GlanceModifier.width(7.dp))
        if (event.time != null) {
            Text(
                text = event.time,
                style = textStyle(timeColor, timeSize, settings.timeBold, settings.eventLight),
            )
        }
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = event.title,
                style = textStyle(color, size, settings.eventBold, settings.eventLight),
                maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            )
            if (event.location != null) {
                Text(
                    text = event.location,
                    style = textStyle(
                        locationColor, locationSize, settings.locationBold, settings.eventLight
                    ),
                    maxLines = if (settings.singleLineLocation) 1 else Int.MAX_VALUE,
                )
            }
        }
    }
}

/** Sample content for the widget picker */
private fun previewAgenda(context: Context): AgendaData {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val timeFormat = SafeDateFormat(Defaults.timeFormat(context), "HH:mm", Locale.getDefault())
    fun time(day: LocalDate, hour: Int) =
        timeFormat.format(day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()) + " "
    fun event(title: Int, time: String?, color: Int) =
        AgendaEventItem(context.getString(title), null, time, color, passed = false)
    val tomorrow = today.plusDays(1)
    val dateFormat = SafeDateFormat(Defaults.dateFormat, "EEEE", Locale.getDefault())
    val later = today.plusDays(3)
    return AgendaData(
        widgetId = AppWidgetManager.INVALID_APPWIDGET_ID,
        settings = AgendaSettings(timeFormat = Defaults.timeFormat(context), showCalendarColors = true),
        hasPermission = true,
        header = null,
        days = listOf(
            AgendaDayItem(
                context.getString(R.string.today), isToday = true,
                listOf(
                    event(R.string.preview_event_1, time(today, 9), 0xFF2196F3.toInt()),
                    event(R.string.preview_event_2, time(today, 12), 0xFF4CAF50.toInt()),
                ),
                Intent(),
            ),
            AgendaDayItem(
                context.getString(R.string.tomorrow), isToday = false,
                listOf(event(R.string.preview_event_3, time(tomorrow, 16), 0xFFFF9800.toInt())),
                Intent(),
            ),
            AgendaDayItem(
                dateFormat.format(later.atStartOfDay(zone).toInstant().toEpochMilli()),
                isToday = false,
                listOf(event(R.string.preview_event_4, null, 0xFF9C27B0.toInt())),
                Intent(),
            ),
        ),
    )
}
