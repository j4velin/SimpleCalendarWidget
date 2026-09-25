package de.j4velin.calendarWidget.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import de.j4velin.calendarWidget.BuildConfig
import de.j4velin.calendarWidget.MonthWidget
import de.j4velin.calendarWidget.Widget
import de.j4velin.calendarWidget.data.widgetPrefs
import de.j4velin.calendarWidget.log

/** The widget's background, with the launcher's corner radius on Android 12+ */
internal fun GlanceModifier.widgetBackground(color: Int): GlanceModifier {
    val modifier = appWidgetBackground().background(ColorProvider(Color(color)))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        modifier
    }
}

/** Whether the widget is smaller than the given size in any dimension */
@Composable
internal fun isSmallerThan(size: DpSize): Boolean =
    LocalSize.current.let { it.width < size.width || it.height < size.height }

internal val SMALL_AGENDA = DpSize(150.dp, 100.dp)
internal val SMALL_MONTH = DpSize(200.dp, 200.dp)

/**
 * Publishes the generated widget previews shown in the widget picker (Android 15+), once per app
 * version. Older versions use the static preview images.
 */
internal suspend fun publishWidgetPreviews(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    val prefs = context.widgetPrefs()
    if (prefs.getInt(PREVIEW_VERSION_KEY, 0) == BuildConfig.VERSION_CODE) return
    val manager = GlanceAppWidgetManager(context)
    val results = listOf(Widget::class, MonthWidget::class).map {
        try {
            manager.setWidgetPreviews(it)
        } catch (e: Exception) {
            log(e)
            GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED
        }
    }
    // retried on the next app start if rate limited
    if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
        prefs.edit { putInt(PREVIEW_VERSION_KEY, BuildConfig.VERSION_CODE) }
    }
}

private const val PREVIEW_VERSION_KEY = "previews_version"
