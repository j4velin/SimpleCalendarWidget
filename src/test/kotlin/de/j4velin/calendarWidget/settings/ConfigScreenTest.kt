package de.j4velin.calendarWidget.settings

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.Backup
import de.j4velin.calendarWidget.data.MonthSettings
import de.j4velin.calendarWidget.data.widgetPrefs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConfigScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private fun string(id: Int) = app.getString(id)

    @Before
    fun grantPermission() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
    }

    @Test
    fun agendaConfigShowsAllTabsAndSaves() {
        val viewModel = ConfigViewModel(
            app, 7, Backup.AGENDA, AgendaSettings::load, AgendaSettings::save,
            { copy(calendarIds = it) }, isNewWidget = false,
        )
        compose.setContent { AppTheme { AgendaConfigScreen(viewModel) } }

        compose.onNodeWithText(string(R.string.calendars)).assertExists()
        compose.onNodeWithText(string(R.string.appearance)).performClick()
        compose.onNodeWithText(string(R.string.eventdate)).assertExists()
        compose.onNodeWithText(string(R.string.settings)).performClick()
        compose.onNodeWithText(string(R.string.todaytomorrow)).performClick()

        viewModel.save()
        assertEquals(false, app.widgetPrefs().getBoolean("todayText_7", true))
        assertEquals(false, AgendaSettings.load(app, 7).todayTomorrowText)
    }

    @Test
    fun monthConfig() {
        val viewModel = ConfigViewModel(
            app, 8, Backup.MONTH, MonthSettings::load, MonthSettings::save,
            { copy(calendarIds = it) }, isNewWidget = false,
        )
        compose.setContent { AppTheme { MonthConfigScreen(viewModel) } }

        val startMonday = viewModel.settings.startOnMonday
        compose.onNodeWithText(string(R.string.week_start)).performClick()
        compose.onNodeWithText(string(R.string.indicators)).performScrollTo().assertExists()

        viewModel.save()
        assertEquals(!startMonday, MonthSettings.load(app, 8).startOnMonday)
    }

    @Test
    fun colorPickerDialog() {
        var selected = 0
        compose.setContent {
            AppTheme {
                ColorPickerDialog(0xFF112233.toInt(), withAlpha = false, onDismiss = {}) {
                    selected = it
                }
            }
        }
        compose.onNodeWithText("112233").assertExists()
        compose.onNodeWithText(app.getString(android.R.string.ok)).performClick()
        assertEquals(0xFF112233.toInt(), selected)
    }
}
