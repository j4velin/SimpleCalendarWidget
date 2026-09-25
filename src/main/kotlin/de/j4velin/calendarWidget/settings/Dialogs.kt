package de.j4velin.calendarWidget.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import de.j4velin.calendarWidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PRESET_COLORS = intArrayOf(
    0xFFFFFFFF.toInt(), 0xFFBDBDBD.toInt(), 0xFF757575.toInt(), 0xFF000000.toInt(),
    0xFFF44336.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt(),
    0xFF009688.toInt(), 0xFF2196F3.toInt(), 0xFF3F51B5.toInt(), 0xFF9C27B0.toInt(),
)

private fun Int.channel(shift: Int) = (this shr shift) and 0xFF
private fun Int.withChannel(shift: Int, value: Int) =
    (this and (0xFF shl shift).inv()) or ((value and 0xFF) shl shift)

private fun Int.toHex(withAlpha: Boolean) =
    if (withAlpha) "%08X".format(this) else "%06X".format(this and 0xFFFFFF)

/** Lets the user pick a color via presets, ARGB sliders or a hex value */
@Composable
internal fun ColorPickerDialog(
    initialColor: Int,
    withAlpha: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    val initial = if (withAlpha) initialColor else initialColor or 0xFF000000.toInt()
    var color by rememberSaveable { mutableIntStateOf(initial) }
    var hex by rememberSaveable { mutableStateOf(initial.toHex(withAlpha)) }
    fun setColor(new: Int) {
        color = if (withAlpha) new else new or 0xFF000000.toInt()
        hex = color.toHex(withAlpha)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onColorSelected(color) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ColorSwatch(color, modifier = Modifier.fillMaxWidth().height(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                PRESET_COLORS.toList().chunked(6).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        row.forEach { preset ->
                            ColorSwatch(
                                preset,
                                modifier = Modifier.size(32.dp).clickable {
                                    setColor(if (withAlpha) preset.withChannel(24, color.channel(24)) else preset)
                                },
                            )
                        }
                    }
                }
                val channels = buildList {
                    if (withAlpha) add("A" to 24)
                    add("R" to 16)
                    add("G" to 8)
                    add("B" to 0)
                }
                channels.forEach { (name, shift) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontFamily = FontFamily.Monospace, modifier = Modifier.width(20.dp))
                        Slider(
                            value = color.channel(shift).toFloat(),
                            onValueChange = { setColor(color.withChannel(shift, it.toInt())) },
                            valueRange = 0f..255f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            color.channel(shift).toString(), fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(36.dp).padding(start = 8.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        val clean = input.removePrefix("#").take(if (withAlpha) 8 else 6).uppercase()
                        hex = clean
                        val parsed = clean.toLongOrNull(16)?.toInt()
                        if (parsed != null && clean.length == (if (withAlpha) 8 else 6)) {
                            color = if (withAlpha) parsed else parsed or 0xFF000000.toInt()
                        }
                    },
                    prefix = { Text("#") },
                    label = { Text(if (withAlpha) "AARRGGBB" else "RRGGBB") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
    )
}

internal data class AppInfo(val label: String, val icon: ImageBitmap, val intentUri: String)

@Suppress("DEPRECATION") // the flags overload requires API 33
private fun loadLauncherApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val iconSize = (40 * context.resources.displayMetrics.density).toInt()
    return pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
    ).map {
        AppInfo(
            label = it.loadLabel(pm).toString(),
            icon = it.loadIcon(pm).toBitmap(iconSize, iconSize).asImageBitmap(),
            intentUri = Intent().setClassName(it.activityInfo.packageName, it.activityInfo.name)
                .toUri(0),
        )
    }.sortedBy { it.label.lowercase() }
}

/** @return the name of the app started by the given intent uri */
@Suppress("DEPRECATION") // the flags overload requires API 33
internal fun appLabel(context: Context, intentUri: String?): String? = try {
    intentUri?.let { Intent.parseUri(it, 0).component }?.let {
        context.packageManager.getActivityInfo(it, 0)
            .loadLabel(context.packageManager).toString()
    }
} catch (e: Exception) {
    null
}

/** Lets the user choose one of the installed apps */
@Composable
internal fun AppPickerDialog(onDismiss: () -> Unit, onAppSelected: (AppInfo) -> Unit) {
    val context = LocalContext.current
    val apps by produceState<List<AppInfo>?>(null) {
        value = withContext(Dispatchers.IO) { loadLauncherApps(context) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_app)) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            val list = apps
            if (list == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(list, key = { it.intentUri }) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAppSelected(app) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                            Text(
                                app.label, style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}
