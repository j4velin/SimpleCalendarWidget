package de.j4velin.calendarWidget.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.CalendarInfo
import de.j4velin.calendarWidget.data.IconColor
import de.j4velin.calendarWidget.widget.SafeDateFormat
import java.util.Locale

/** A scrollable settings page */
@Composable
internal fun SettingsPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            // the bottom padding keeps the last setting clear of the done button
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        content = content,
    )
}

@Composable
internal fun SectionTitle(text: String) {
    Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun Description(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
internal fun CheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun RadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
        Column {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shows a color, with a checkerboard pattern behind it to visualize transparency */
@Composable
internal fun ColorSwatch(color: Int, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            .drawBehind {
                val cell = 6.dp.toPx()
                val columns = (size.width / cell).toInt() + 1
                val rows = (size.height / cell).toInt() + 1
                drawRect(Color.White)
                for (x in 0 until columns) for (y in 0 until rows) {
                    if ((x + y) % 2 == 0) {
                        drawRect(Color.LightGray, Offset(x * cell, y * cell), Size(cell, cell))
                    }
                }
            }
            .background(Color(color))
            .border(1.dp, outline, RoundedCornerShape(8.dp))
    )
}

/** A color setting: label and swatch, opening a color picker on click */
@Composable
internal fun ColorSetting(
    label: String,
    color: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    withAlpha: Boolean = false,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.clickable { showPicker = true }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(12.dp))
        ColorSwatch(color)
    }
    if (showPicker) {
        ColorPickerDialog(
            initialColor = color,
            withAlpha = withAlpha,
            onDismiss = { showPicker = false },
            onColorSelected = {
                onColorChange(it)
                showPicker = false
            },
        )
    }
}

private fun Float.formatSize(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

@Composable
internal fun SizeField(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf(value.formatSize()) }
    val parsed = text.replace(',', '.').toFloatOrNull()?.takeIf { it > 0 }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.replace(',', '.').toFloatOrNull()?.takeIf { size -> size > 0 }?.let(onValueChange)
        },
        label = { Text(stringResource(R.string.size).trimEnd(':')) },
        isError = parsed == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.width(110.dp),
    )
}

/** Color and size of a text element */
@Composable
internal fun ColorAndSize(
    color: Int,
    onColorChange: (Int) -> Unit,
    size: Float,
    onSizeChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ColorSetting(stringResource(R.string.color), color, onColorChange)
        SizeField(size, onSizeChange)
    }
}

@Composable
internal fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.takeIf { number -> number > 0 }?.let(onValueChange)
        },
        label = { Text(label) },
        isError = text.toIntOrNull()?.takeIf { it > 0 } == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

internal val DATE_FORMAT_PRESETS = listOf(
    "EEEE, dd. MMMM", "EEEE, MMMM/dd", "EEEE, dd. MMM", "EEEE, MMM/dd", "EEE, dd.MM.",
    "EEE, MM/dd", "dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "d MMMM", "EEEE",
)
internal val TIME_FORMAT_PRESETS = listOf("HH:mm", "H:mm", "hh:mm a", "h:mm a", "h:mma")
internal val SHORT_DATE_FORMAT_PRESETS = listOf("dd.MM.", "MM/dd", "dd.MM.yyyy", "d. MMM", "MMM d")

/**
 * Editable date format pattern with a list of presets and a preview. Only valid patterns are
 * passed to [onValueChange].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateFormatField(
    label: String,
    value: String,
    presets: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(value) }
    var expanded by remember { mutableStateOf(false) }
    val valid = text.isNotBlank() && SafeDateFormat.isValid(text)
    val now = remember { System.currentTimeMillis() }
    fun format(pattern: String) = SafeDateFormat(pattern, pattern, Locale.getDefault()).format(now)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                if (it.isNotBlank() && SafeDateFormat.isValid(it)) onValueChange(it)
            },
            label = { Text(label.trimEnd(':')) },
            singleLine = true,
            isError = !valid,
            supportingText = {
                Text(if (valid) format(text) else stringResource(R.string.invalid_format))
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(format(preset))
                            Text(
                                preset, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        text = preset
                        onValueChange(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** A read-only dropdown to select one of [options] */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Dropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label.trimEnd(':')) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** List of the device's calendars to choose from */
@Composable
internal fun CalendarSelection(
    calendars: List<CalendarInfo>?,
    selected: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
) {
    when {
        calendars == null -> Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        calendars.isEmpty() -> Description(stringResource(R.string.no_calendars))

        else -> calendars.forEach { calendar ->
            CheckboxRow(
                text = "${calendar.name} (${calendar.account})",
                checked = calendar.id in selected,
                onCheckedChange = {
                    onSelectionChange(if (it) selected + calendar.id else selected - calendar.id)
                },
                leading = {
                    Box(
                        modifier = Modifier.size(width = 6.dp, height = 32.dp)
                            .clip(RoundedCornerShape(3.dp)).background(Color(calendar.color))
                    )
                },
            )
        }
    }
}

/** Color and transparency of the widget's icons */
@Composable
internal fun IconSettings(
    iconColor: IconColor,
    iconAlpha: Int,
    previewIcon: Int,
    onColorChange: (IconColor) -> Unit,
    onAlphaChange: (Int) -> Unit,
) {
    val colorNames = listOf(stringResource(R.string.black), stringResource(R.string.white))
    Dropdown(
        label = stringResource(R.string.color),
        options = colorNames,
        selectedIndex = IconColor.entries.indexOf(iconColor),
        onSelect = { onColorChange(IconColor.entries[it]) },
        modifier = Modifier.padding(vertical = 4.dp),
    )
    Text(
        text = stringResource(R.string.transparency),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = (255 - iconAlpha).toFloat(),
            onValueChange = { onAlphaChange(255 - it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.padding(start = 12.dp).size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(if (iconColor == IconColor.WHITE) Color.DarkGray else Color.LightGray),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(previewIcon),
                contentDescription = null,
                alpha = iconAlpha / 255f,
                colorFilter = ColorFilter.tint(Color(iconColor.color)),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
