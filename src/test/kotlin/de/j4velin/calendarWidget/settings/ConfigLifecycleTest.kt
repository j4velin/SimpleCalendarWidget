package de.j4velin.calendarWidget.settings

import android.Manifest
import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.AgendaSettings
import de.j4velin.calendarWidget.data.Backup
import de.j4velin.calendarWidget.data.widgetPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Serves two visible and one hidden calendar */
class FakeCalendarProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor = MatrixCursor(projection).apply {
        addRow(arrayOf<Any>(1L, "Work", "me@example.com", 0xFF2196F3.toInt(), 1))
        addRow(arrayOf<Any>(2L, "Holidays", "me@example.com", 0xFF4CAF50.toInt(), 0))
        addRow(arrayOf<Any>(3L, "Private", "me@example.com", 0xFFFF9800.toInt(), 1))
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?,
    ) = 0
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConfigLifecycleTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        Robolectric.buildContentProvider(FakeCalendarProvider::class.java).create("com.android.calendar")
    }

    @Test
    fun editingKeepsTheSelectedCalendars() {
        AgendaSettings(timeFormat = "HH:mm", calendarIds = setOf(2L)).save(app.widgetPrefs(), 14)
        val scenario = ActivityScenario.launchActivityForResult<WidgetConfig>(
            WidgetConfig.editIntent(app, 14)
        )
        compose.onNodeWithText("Work (me@example.com)").assertExists()
        compose.onNodeWithContentDescription(app.getString(R.string.done)).performClick()
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        assertEquals(setOf(2L), AgendaSettings.load(app, 14).calendarIds)
    }

    private fun placeWidget(id: Int) = ActivityScenario.launchActivityForResult<WidgetConfig>(
        Intent(app, WidgetConfig::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    )

    /** also checks that a new widget shows all visible calendars by default */
    @Test
    fun doneSavesAndPlacesTheWidget() {
        val scenario = placeWidget(11)
        compose.onNodeWithContentDescription(app.getString(R.string.done)).performClick()
        assertEquals(Activity.RESULT_OK, scenario.result.resultCode)
        assertEquals(setOf(1L, 3L), AgendaSettings.load(app, 11).calendarIds)
    }

    private fun string(id: Int) = app.getString(id)

    private fun pressCancel() =
        compose.onNodeWithContentDescription(string(android.R.string.cancel)).performClick()

    @Test
    fun cancelAsksAndThenDiscardsTheNewWidget() {
        val scenario = placeWidget(12)
        pressCancel()
        compose.onNodeWithText(string(R.string.discard_new_widget)).assertExists()
        compose.onNodeWithText(string(R.string.discard)).performClick()
        assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        assertFalse(app.widgetPrefs().all.keys.any { it.endsWith("_12") })
    }

    @Test
    fun backAsksAndKeepEditingStays() {
        val scenario = placeWidget(13)
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(string(R.string.keep_editing)).performClick()
        compose.onNodeWithText(string(R.string.discard_changes)).assertDoesNotExist()
        scenario.onActivity { assertFalse(it.isFinishing) }
        assertTrue(app.widgetPrefs().all.keys.none { it.endsWith("_13") })
    }

    @Test
    fun editingWithoutChangesClosesWithoutAsking() {
        AgendaSettings(timeFormat = "HH:mm", calendarIds = setOf(1L)).save(app.widgetPrefs(), 15)
        val scenario = ActivityScenario.launchActivityForResult<WidgetConfig>(
            WidgetConfig.editIntent(app, 15)
        )
        compose.onNodeWithText("Work (me@example.com)").assertExists()
        pressCancel()
        compose.onNodeWithText(string(R.string.discard_changes)).assertDoesNotExist()
        assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
    }

    @Test
    fun editingWithChangesAsksAndDiscards() {
        AgendaSettings(timeFormat = "HH:mm", calendarIds = setOf(1L)).save(app.widgetPrefs(), 16)
        val scenario = ActivityScenario.launchActivityForResult<WidgetConfig>(
            WidgetConfig.editIntent(app, 16)
        )
        compose.onNodeWithText("Private (me@example.com)").performClick()
        pressCancel()
        compose.onNodeWithText(string(R.string.discard_new_widget)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.discard)).performClick()
        assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        assertEquals(setOf(1L), AgendaSettings.load(app, 16).calendarIds)
    }
}
