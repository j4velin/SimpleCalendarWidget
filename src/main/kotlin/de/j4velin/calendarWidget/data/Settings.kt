package de.j4velin.calendarWidget.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.text.format.DateFormat
import androidx.core.content.edit
import java.util.Locale

/**
 * All widget settings live in this SharedPreferences file, each key is suffixed with the widget id.
 * The keys must not be changed, otherwise existing widgets and backups lose their configuration.
 */
internal const val PREFS_NAME = "calendarWidget"

internal fun Context.widgetPrefs(): SharedPreferences =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

enum class IconColor(val prefValue: Int, val color: Int) {
    BLACK(1, Color.BLACK), WHITE(2, Color.WHITE);

    companion object {
        fun fromPref(value: Int) = entries.firstOrNull { it.prefValue == value } ?: WHITE
    }
}

enum class ClickAction { DEFAULT_APP, OTHER_APP, UPDATE }

internal object Defaults {
    const val BG_COLOR = 1694498816 // black, ~40% transparent
    const val DATE_COLOR = Color.WHITE
    const val PASSED_COLOR = Color.GRAY
    const val EVENT_COLOR = Color.WHITE
    const val TIME_COLOR = Color.WHITE
    const val LOCATION_COLOR = Color.WHITE

    const val DATE_SIZE = 14f
    const val EVENT_SIZE = 12f
    const val TIME_SIZE = 12f
    const val TODAY_SIZE = 20f
    const val LOCATION_SIZE = 10f

    private val isEnglish
        get() = Locale.getDefault().language == Locale.ENGLISH.language

    val dateFormat get() = if (isEnglish) "EEEE, MMM/dd" else "EEEE, dd. MMM"
    val todayDateFormat get() = if (isEnglish) "EEEE, MMMM/dd" else "EEEE, dd. MMMM"
    val alldayEndFormat get() = if (isEnglish) "MM/dd" else "dd.MM."
    fun timeFormat(context: Context) = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"

    val startOnMonday get() = !Locale.getDefault().country.equals(Locale.US.country, ignoreCase = true)
}

/** Look ahead units, in seconds, same order as R.array.lookAheadTimes */
internal val LOOK_AHEAD_UNIT_SECONDS = longArrayOf(86_400, 604_800, 2_678_400, 31_536_000)

private const val LAYOUT_NONE_LIGHT = 0
private const val LAYOUT_TITLE_LIGHT = 1
private const val LAYOUT_TEXT_LIGHT = 2
private const val LAYOUT_ALL_LIGHT = 3

/** Settings of the agenda widget */
data class AgendaSettings(
    // events
    val calendarIds: Set<Long> = emptySet(),
    val lookAheadUnit: Int = 1,
    val lookAheadTime: Int = 4,
    val showCalendarColors: Boolean = false,
    // appearance
    val showCurrentDate: Boolean = false,
    val currentDateFormat: String = Defaults.todayDateFormat,
    val currentDateColor: Int = Defaults.DATE_COLOR,
    val currentDateSize: Float = Defaults.DATE_SIZE,
    val currentDateBold: Boolean = false,
    val dateFormat: String = Defaults.dateFormat,
    val dateColor: Int = Defaults.DATE_COLOR,
    val dateSize: Float = Defaults.DATE_SIZE,
    val dateBold: Boolean = true,
    val dateLight: Boolean = false,
    val timeFormat: String,
    val timeColor: Int = Defaults.TIME_COLOR,
    val timeSize: Float = Defaults.TIME_SIZE,
    val timeBold: Boolean = false,
    val showEndTimes: Boolean = false,
    val eventColor: Int = Defaults.EVENT_COLOR,
    val eventSize: Float = Defaults.EVENT_SIZE,
    val eventBold: Boolean = false,
    val eventLight: Boolean = true,
    val singleLineEvent: Boolean = false,
    val showLocation: Boolean = false,
    val locationColor: Int = Defaults.LOCATION_COLOR,
    val locationSize: Float = Defaults.LOCATION_SIZE,
    val locationBold: Boolean = false,
    val singleLineLocation: Boolean = false,
    val todayEventColor: Int = Defaults.EVENT_COLOR,
    val todayEventSize: Float = Defaults.TODAY_SIZE,
    val singleLineToday: Boolean = false,
    val skipPassed: Boolean = true,
    val todayPassedEventColor: Int = Defaults.PASSED_COLOR,
    val todayPassedEventSize: Float = Defaults.EVENT_SIZE,
    val singleLineTodayPassed: Boolean = false,
    val timeLocationTodaySameAsEvent: Boolean = true,
    val backgroundColor: Int = Defaults.BG_COLOR,
    // settings
    val todayTomorrowText: Boolean = true,
    /** show multi-day events on every day or only once, with an end date in this format */
    val multiDayOnEveryDay: Boolean = true,
    val alldayEndFormat: String = Defaults.alldayEndFormat,
    val clickAction: ClickAction = ClickAction.DEFAULT_APP,
    /** intent uri of the app to start, if [clickAction] is [ClickAction.OTHER_APP] */
    val otherApp: String? = null,
    val openSingleEvent: Boolean = false,
    val iconColor: IconColor = IconColor.WHITE,
    val iconAlpha: Int = 255,
) {
    /** The point in time until events are shown, relative to [from] */
    fun lookAheadEnd(from: Long): Long =
        from + LOOK_AHEAD_UNIT_SECONDS[lookAheadUnit.coerceIn(0, 3)] * lookAheadTime * 1000L

    fun save(prefs: SharedPreferences, widgetId: Int) = prefs.edit {
        putString("cals_$widgetId", CalendarIds.format(calendarIds))
        putInt("lookaheadUnit_$widgetId", lookAheadUnit)
        putInt("lookaheadtime_$widgetId", lookAheadTime)
        putBoolean("showCalendarColors_$widgetId", showCalendarColors)

        putString("today_$widgetId", if (showCurrentDate) currentDateFormat else null)
        putInt("current_date_color_$widgetId", currentDateColor)
        putFloat("current_date_size_$widgetId", currentDateSize)
        putBoolean("current_date_bold_$widgetId", currentDateBold)
        putString("dateformat_$widgetId", dateFormat)
        putInt("title_$widgetId", dateColor)
        putFloat("titlesize_$widgetId", dateSize)
        putBoolean("datebold_$widgetId", dateBold)
        putInt(
            "layout_$widgetId", when {
                dateLight && eventLight -> LAYOUT_ALL_LIGHT
                dateLight -> LAYOUT_TITLE_LIGHT
                eventLight -> LAYOUT_TEXT_LIGHT
                else -> LAYOUT_NONE_LIGHT
            }
        )
        putString("timeformat_$widgetId", timeFormat)
        putInt("time_$widgetId", timeColor)
        putFloat("timesize_$widgetId", timeSize)
        putBoolean("timebold_$widgetId", timeBold)
        putBoolean("endTimes_$widgetId", showEndTimes)
        putInt("text_$widgetId", eventColor)
        putFloat("textsize_$widgetId", eventSize)
        putBoolean("eventbold_$widgetId", eventBold)
        putBoolean("singleLineEvent_$widgetId", singleLineEvent)
        putBoolean("showLocation_$widgetId", showLocation)
        putInt("location_$widgetId", locationColor)
        putFloat("locationsize_$widgetId", locationSize)
        putBoolean("locationbold_$widgetId", locationBold)
        putBoolean("singleLineLocation_$widgetId", singleLineLocation)
        putInt("today_text_$widgetId", todayEventColor)
        putFloat("today_textsize_$widgetId", todayEventSize)
        putBoolean("singleLineToday_$widgetId", singleLineToday)
        putBoolean("skipPassed_$widgetId", skipPassed)
        putInt("today_passed_text_$widgetId", todayPassedEventColor)
        putFloat("today_passed_textsize_$widgetId", todayPassedEventSize)
        putBoolean("singleLineTodayPassed_$widgetId", singleLineTodayPassed)
        putBoolean("timeLocationTodaySameAsEvent_$widgetId", timeLocationTodaySameAsEvent)
        putInt("bg_$widgetId", backgroundColor)

        putBoolean("todayText_$widgetId", todayTomorrowText)
        putString("allday_endformat_$widgetId", if (multiDayOnEveryDay) "" else alldayEndFormat)
        if (clickAction == ClickAction.OTHER_APP && otherApp != null) {
            putString("otherApp_$widgetId", otherApp)
        } else {
            remove("otherApp_$widgetId")
        }
        putBoolean("openCalendar_$widgetId", clickAction == ClickAction.DEFAULT_APP)
        remove("update_$widgetId") // no longer used
        putBoolean("openEvent_$widgetId", openSingleEvent)
        putInt("icon_$widgetId", iconColor.prefValue)
        putInt("icon_alpha_$widgetId", iconAlpha)
    }

    companion object {
        fun load(context: Context, widgetId: Int): AgendaSettings {
            val prefs = context.widgetPrefs()
            val layout = prefs.getInt("layout_$widgetId", LAYOUT_TEXT_LIGHT)
            val alldayEndFormat = prefs.getString("allday_endformat_$widgetId", "").orEmpty()
            val today = prefs.getString("today_$widgetId", null)
            val otherApp = prefs.getString("otherApp_$widgetId", null)
            return AgendaSettings(
                calendarIds = CalendarIds.parse(prefs.getString("cals_$widgetId", null)),
                lookAheadUnit = prefs.getInt("lookaheadUnit_$widgetId", 1),
                lookAheadTime = prefs.getInt("lookaheadtime_$widgetId", 4),
                showCalendarColors = prefs.getBoolean("showCalendarColors_$widgetId", false),

                showCurrentDate = today != null,
                currentDateFormat = today ?: Defaults.todayDateFormat,
                currentDateColor = prefs.getInt("current_date_color_$widgetId", Defaults.DATE_COLOR),
                currentDateSize = prefs.getFloat("current_date_size_$widgetId", Defaults.DATE_SIZE),
                currentDateBold = prefs.getBoolean("current_date_bold_$widgetId", false),
                dateFormat = prefs.getString("dateformat_$widgetId", null) ?: Defaults.dateFormat,
                dateColor = prefs.getInt("title_$widgetId", Defaults.DATE_COLOR),
                dateSize = prefs.getFloat("titlesize_$widgetId", Defaults.DATE_SIZE),
                dateBold = prefs.getBoolean("datebold_$widgetId", true),
                dateLight = layout == LAYOUT_TITLE_LIGHT || layout == LAYOUT_ALL_LIGHT,
                timeFormat = prefs.getString("timeformat_$widgetId", null)
                    ?: Defaults.timeFormat(context),
                timeColor = prefs.getInt("time_$widgetId", Defaults.TIME_COLOR),
                timeSize = prefs.getFloat("timesize_$widgetId", Defaults.TIME_SIZE),
                timeBold = prefs.getBoolean("timebold_$widgetId", false),
                showEndTimes = prefs.getBoolean("endTimes_$widgetId", false),
                eventColor = prefs.getInt("text_$widgetId", Defaults.EVENT_COLOR),
                eventSize = prefs.getFloat("textsize_$widgetId", Defaults.EVENT_SIZE),
                eventBold = prefs.getBoolean("eventbold_$widgetId", false),
                eventLight = layout == LAYOUT_TEXT_LIGHT || layout == LAYOUT_ALL_LIGHT,
                singleLineEvent = prefs.getBoolean("singleLineEvent_$widgetId", false),
                showLocation = prefs.getBoolean("showLocation_$widgetId", false),
                locationColor = prefs.getInt("location_$widgetId", Defaults.LOCATION_COLOR),
                locationSize = prefs.getFloat("locationsize_$widgetId", Defaults.LOCATION_SIZE),
                locationBold = prefs.getBoolean("locationbold_$widgetId", false),
                singleLineLocation = prefs.getBoolean("singleLineLocation_$widgetId", false),
                todayEventColor = prefs.getInt("today_text_$widgetId", Defaults.EVENT_COLOR),
                todayEventSize = prefs.getFloat("today_textsize_$widgetId", Defaults.TODAY_SIZE),
                singleLineToday = prefs.getBoolean("singleLineToday_$widgetId", false),
                skipPassed = prefs.getBoolean("skipPassed_$widgetId", true),
                todayPassedEventColor = prefs.getInt(
                    "today_passed_text_$widgetId", Defaults.PASSED_COLOR
                ),
                todayPassedEventSize = prefs.getFloat(
                    "today_passed_textsize_$widgetId", Defaults.EVENT_SIZE
                ),
                singleLineTodayPassed = prefs.getBoolean("singleLineTodayPassed_$widgetId", false),
                timeLocationTodaySameAsEvent = prefs.getBoolean(
                    "timeLocationTodaySameAsEvent_$widgetId", true
                ),
                backgroundColor = prefs.getInt("bg_$widgetId", Defaults.BG_COLOR),

                todayTomorrowText = prefs.getBoolean("todayText_$widgetId", true),
                multiDayOnEveryDay = alldayEndFormat.isEmpty(),
                alldayEndFormat = alldayEndFormat.ifEmpty { Defaults.alldayEndFormat },
                clickAction = when {
                    prefs.getBoolean("openCalendar_$widgetId", true) -> ClickAction.DEFAULT_APP
                    otherApp != null -> ClickAction.OTHER_APP
                    else -> ClickAction.UPDATE
                },
                otherApp = otherApp,
                openSingleEvent = prefs.getBoolean("openEvent_$widgetId", false),
                iconColor = IconColor.fromPref(prefs.getInt("icon_$widgetId", 2)),
                iconAlpha = prefs.getInt("icon_alpha_$widgetId", 255),
            )
        }
    }
}

/** Settings of the month widget */
data class MonthSettings(
    val calendarIds: Set<Long> = emptySet(),
    val startOnMonday: Boolean = Defaults.startOnMonday,
    val todayTextColor: Int = Color.BLACK,
    val todayBackgroundColor: Int = Color.WHITE,
    val monthTextColor: Int = Color.WHITE,
    val monthBackgroundColor: Int = Color.argb(75, 255, 255, 255),
    val otherTextColor: Int = Color.LTGRAY,
    val otherBackgroundColor: Int = Color.TRANSPARENT,
    val dayLabelColor: Int = Color.LTGRAY,
    val monthLabelColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.TRANSPARENT,
    val iconColor: IconColor = IconColor.WHITE,
    val iconAlpha: Int = 255,
    /** the displayed month, relative to the current one */
    val monthOffset: Int = 0,
    val openCalendar: Boolean = true,
) {
    /** Saves the settings and resets the widget to show the current month */
    fun save(prefs: SharedPreferences, widgetId: Int) = prefs.edit {
        putString("cals_$widgetId", CalendarIds.format(calendarIds))
        putInt("month_offset_$widgetId", 0)
        putBoolean("start_monday_$widgetId", startOnMonday)
        putInt("today_text_$widgetId", todayTextColor)
        putInt("today_bg_$widgetId", todayBackgroundColor)
        putInt("month_text_$widgetId", monthTextColor)
        putInt("month_bg_$widgetId", monthBackgroundColor)
        putInt("other_text_$widgetId", otherTextColor)
        putInt("other_bg_$widgetId", otherBackgroundColor)
        putInt("dayslabel_text_$widgetId", dayLabelColor)
        putInt("monthlabel_text_$widgetId", monthLabelColor)
        putInt("monthwidget_bg_$widgetId", backgroundColor)
        putInt("icon_$widgetId", iconColor.prefValue)
        putInt("icon_alpha_$widgetId", iconAlpha)
    }

    companion object {
        fun load(context: Context, widgetId: Int): MonthSettings {
            val prefs = context.widgetPrefs()
            val defaults = MonthSettings()
            return MonthSettings(
                calendarIds = CalendarIds.parse(prefs.getString("cals_$widgetId", null)),
                startOnMonday = prefs.getBoolean("start_monday_$widgetId", defaults.startOnMonday),
                todayTextColor = prefs.getInt("today_text_$widgetId", defaults.todayTextColor),
                todayBackgroundColor = prefs.getInt(
                    "today_bg_$widgetId", defaults.todayBackgroundColor
                ),
                monthTextColor = prefs.getInt("month_text_$widgetId", defaults.monthTextColor),
                monthBackgroundColor = prefs.getInt(
                    "month_bg_$widgetId", defaults.monthBackgroundColor
                ),
                otherTextColor = prefs.getInt("other_text_$widgetId", defaults.otherTextColor),
                otherBackgroundColor = prefs.getInt(
                    "other_bg_$widgetId", defaults.otherBackgroundColor
                ),
                dayLabelColor = prefs.getInt("dayslabel_text_$widgetId", defaults.dayLabelColor),
                monthLabelColor = prefs.getInt(
                    "monthlabel_text_$widgetId", defaults.monthLabelColor
                ),
                backgroundColor = prefs.getInt("monthwidget_bg_$widgetId", defaults.backgroundColor),
                iconColor = IconColor.fromPref(prefs.getInt("icon_$widgetId", 2)),
                iconAlpha = prefs.getInt("icon_alpha_$widgetId", 255),
                monthOffset = prefs.getInt("month_offset_$widgetId", 0),
                openCalendar = prefs.getBoolean("openCalendar_$widgetId", true) ||
                        prefs.contains("otherApp_$widgetId"),
            )
        }

        fun changeMonthOffset(context: Context, widgetId: Int, delta: Int) {
            val prefs = context.widgetPrefs()
            prefs.edit {
                putInt("month_offset_$widgetId", prefs.getInt("month_offset_$widgetId", 0) + delta)
            }
        }
    }
}
