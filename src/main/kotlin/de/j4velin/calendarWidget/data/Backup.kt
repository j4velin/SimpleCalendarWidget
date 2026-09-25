package de.j4velin.calendarWidget.data

import android.content.Context
import androidx.core.content.edit
import de.j4velin.calendarWidget.log
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Backup & restore of a widget's settings to the app's external files directory
 * (`Android/data/de.j4velin.calendarWidget/files`).
 *
 * The file is a serialized `HashMap<String, Any>` of all settings keys without the widget id,
 * the format is compatible with backups created by older versions.
 */
internal object Backup {

    const val AGENDA = "agenda"
    const val MONTH = "month"

    fun file(context: Context, type: String): File? =
        context.getExternalFilesDir(null)?.let { File(it, "backup_$type") }

    /** @return the file the backup was written to, null on error */
    fun save(context: Context, widgetId: Int, type: String): File? {
        val file = file(context, type) ?: return null
        val suffix = "_$widgetId"
        val entries = HashMap<String, Any>()
        context.widgetPrefs().all.forEach { (key, value) ->
            if (key.endsWith(suffix) && value != null) {
                entries[key.substring(0, key.length - suffix.length + 1)] = value
            }
        }
        return try {
            ObjectOutputStream(file.outputStream()).use { it.writeObject(entries) }
            file
        } catch (e: Exception) {
            log(e)
            null
        }
    }

    enum class RestoreResult { RESTORED, NO_BACKUP, ERROR }

    fun restore(context: Context, widgetId: Int, type: String): RestoreResult {
        val dir = context.getExternalFilesDir(null) ?: return RestoreResult.ERROR
        // older versions wrote a single "backup" file for both widget types
        val file = File(dir, "backup_$type").takeIf { it.exists() }
            ?: File(dir, "backup").takeIf { it.exists() }
            ?: return RestoreResult.NO_BACKUP
        return try {
            val entries = ObjectInputStream(file.inputStream()).use { it.readObject() } as Map<*, *>
            context.widgetPrefs().edit {
                entries.forEach { (key, value) ->
                    val name = "$key$widgetId"
                    when (value) {
                        is Boolean -> putBoolean(name, value)
                        is Float -> putFloat(name, value)
                        is Int -> putInt(name, value)
                        is Long -> putLong(name, value)
                        is String -> putString(name, value)
                    }
                }
            }
            RestoreResult.RESTORED
        } catch (e: Exception) {
            log(e)
            RestoreResult.ERROR
        }
    }
}
