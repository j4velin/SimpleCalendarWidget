package de.j4velin.calendarWidget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import de.j4velin.calendarWidget.settings.AppTheme

/**
 * Shown when the app is started from the launcher: explains how to add the widget and removes the
 * launcher entry afterwards.
 */
class Dummy : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AlertDialog(
                    onDismissRequest = ::finish,
                    title = { Text(stringResource(R.string.app_name)) },
                    text = { Text(stringResource(R.string.launcher_info)) },
                    confirmButton = {
                        TextButton(onClick = ::finish) { Text(stringResource(android.R.string.ok)) }
                    },
                )
            }
        }
        packageManager.setComponentEnabledSetting(
            componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

/** Imports the first event of an .ics file by handing it to the calendar app */
class IcsImporter : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data
        if (intent.action == Intent.ACTION_VIEW && data != null) {
            try {
                val event = contentResolver.openInputStream(data)?.bufferedReader()?.use {
                    IcsParser.parse(it.lineSequence())
                }
                if (event == null) {
                    Toast.makeText(this, R.string.ics_no_event, Toast.LENGTH_LONG).show()
                } else {
                    startActivity(Intent(Intent.ACTION_INSERT).apply {
                        setData(CalendarContract.Events.CONTENT_URI)
                        event.begin?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
                        event.end?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
                        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, event.allDay)
                        putExtra(CalendarContract.Events.TITLE, event.title)
                        putExtra(CalendarContract.Events.DESCRIPTION, event.description)
                        putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                    })
                }
            } catch (e: ActivityNotFoundException) {
                log(e)
                Toast.makeText(this, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                log(e)
                Toast.makeText(
                    this, "Error: ${e.javaClass.simpleName}\n${e.message}", Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }
}
