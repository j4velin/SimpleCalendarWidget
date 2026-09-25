package de.j4velin.calendarWidget.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.composeForPreview
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.MonthGrid
import de.j4velin.calendarWidget.data.MonthSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders the widgets' Glance content into RemoteViews and inflates them, which fails if the
 * layout violates any of the RemoteViews restrictions.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class WidgetRenderingTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit): View =
        runBlocking {
            GlanceRemoteViews().compose(context, DpSize(300.dp, 400.dp), content = content)
                .remoteViews.apply(context, FrameLayout(context))
        }

    private fun View.texts(): List<String> = when (this) {
        is TextView -> listOf(text.toString())
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).texts() }
        else -> emptyList()
    }

    @Test
    fun agendaWidget() {
        val day = AgendaDayItem(
            title = "Today",
            isToday = true,
            events = listOf(AgendaEventItem("Meeting", "Room 1", "10:00 ", 0xFF2196F3.toInt(), false)),
            click = Intent(),
        )
        val data = AgendaData(
            widgetId = 1,
            settings = AgendaSettings(
                timeFormat = "HH:mm", showCurrentDate = true, showCalendarColors = true,
                showLocation = true,
            ),
            hasPermission = true,
            header = "Friday, 25. September",
            days = listOf(day),
        )
        val texts = render { AgendaContent(data) }.texts()
        assertTrue(texts.toString(), "Friday, 25. September" in texts)
    }

    @Test
    fun generatedPreviews() = runBlocking {
        val category = android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
        AgendaGlanceWidget().composeForPreview(context, category).apply(context, FrameLayout(context))
        val month = MonthGlanceWidget().composeForPreview(context, category)
            .apply(context, FrameLayout(context)).texts()
        assertEquals(42, month.count { it.toIntOrNull() != null })
    }

    @Test
    fun agendaWidgetWithoutPermission() {
        val data = AgendaData(
            widgetId = 1, settings = AgendaSettings(timeFormat = "HH:mm"), hasPermission = false,
            header = null, days = emptyList(),
        )
        val texts = render { AgendaContent(data) }.texts()
        assertTrue(texts.toString(), "No permission to read from Calendar" in texts)
    }

    @Test
    fun monthWidget() {
        val today = LocalDate.of(2026, 9, 25)
        val grid = MonthGrid.of(today, 0, startOnMonday = true)
        val data = MonthData(
            widgetId = 1,
            settings = MonthSettings(startOnMonday = true),
            grid = grid,
            today = today,
            label = "September 2026",
            weekDayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
            eventCounts = mapOf(today to 2),
            zone = ZoneId.of("Europe/Berlin"),
        )
        val texts = render { MonthContent(data) }.texts()
        assertTrue("September 2026" in texts)
        assertTrue(texts.containsAll(data.weekDayLabels))
        // 42 day numbers, plus the event indicator
        assertEquals(42, texts.count { it.toIntOrNull() != null })
        assertTrue(".." in texts)
    }
}
