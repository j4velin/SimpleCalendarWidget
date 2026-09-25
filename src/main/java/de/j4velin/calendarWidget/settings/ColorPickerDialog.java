package de.j4velin.calendarWidget.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Simple hex based color input, replaces the former external colorpicker library
 */
public class ColorPickerDialog {

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    private final Context context;
    private final int initialColor;
    private boolean alphaEnabled;
    private OnColorChangedListener listener;

    public ColorPickerDialog(final Context context, final int initialColor) {
        this.context = context;
        this.initialColor = initialColor;
    }

    public void setHexValueEnabled(final boolean enabled) {
        // hex input is the only input method
    }

    public void setAlphaSliderVisible(final boolean visible) {
        alphaEnabled = visible;
    }

    public void setOnColorChangedListener(final OnColorChangedListener listener) {
        this.listener = listener;
    }

    public void show() {
        final EditText input = new EditText(context);
        input.setSingleLine();
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(alphaEnabled ? 9 : 7)});
        input.setText(alphaEnabled ? String.format("#%08X", initialColor) :
                String.format("#%06X", initialColor & 0xFFFFFF));
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(context).setTitle(alphaEnabled ? "#AARRGGBB" : "#RRGGBB")
                .setView(input).setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try {
                        int color = Color.parseColor(input.getText().toString().trim());
                        if (!alphaEnabled) color |= 0xFF000000;
                        if (listener != null) listener.onColorChanged(color);
                    } catch (IllegalArgumentException e) {
                        Toast.makeText(context, "Invalid color", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }
}
