package de.j4velin.calendarWidget.settings;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import de.j4velin.calendarWidget.R;

public class Fragment_Events extends WidgetSettingsFragment {

    private CheckBox[] calendars;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.config_calendars, container, false);

        // add calendars
        final Cursor cursor = getActivity().getContentResolver()
                .query(CalendarContract.Calendars.CONTENT_URI,
                        new String[]{CalendarContract.Calendars._ID,
                                CalendarContract.Calendars.ACCOUNT_NAME,
                                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                                CalendarContract.Calendars.CALENDAR_COLOR}, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            calendars = new CheckBox[cursor.getCount()];
            cursor.moveToFirst();
            final LinearLayout cal = (LinearLayout) v.findViewById(R.id.cals);
            LinearLayout row;
            CheckBox box;
            View color;
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(20, LinearLayout.LayoutParams.MATCH_PARENT);
            int pos = 0;
            do {
                row = new LinearLayout(getActivity());
                box = new CheckBox(getActivity());
                if (android.os.Build.VERSION.SDK_INT >= 14) {
                    color = new View(getActivity());
                    color.setBackgroundColor(cursor.getInt(3));
                    color.setLayoutParams(params);
                    row.addView(color);
                }
                calendars[pos] = box;
                box.setText(cursor.getString(2) + " (" + cursor.getString(1) + ")");
                box.setTag(cursor.getString(0));
                row.addView(box);
                cal.addView(row);
                pos++;
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            calendars = new CheckBox[0];
            Toast.makeText(getActivity(), "No calendars found on your device!", Toast.LENGTH_LONG)
                    .show();
            if (!Build.FINGERPRINT.contains("generic")) getActivity().finish();
            return v;
        }

        // look ahead settings
        final Spinner lookAheadSpinner = (Spinner) v.findViewById(R.id.lookaheadspinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(getActivity(), R.array.lookAheadTimes,
                        android.R.layout.simple_spinner_dropdown_item);
        lookAheadSpinner.setAdapter(adapter);
        return v;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        restore(getActivity().getSharedPreferences("calendarWidget", Context.MODE_PRIVATE),
                WidgetConfig.widgetId);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor edit =
                getActivity().getSharedPreferences("calendarWidget", Context.MODE_PRIVATE).edit();
        save(edit, WidgetConfig.widgetId);
        edit.apply();
    }

    @Override
    public void save(final SharedPreferences.Editor edit, final int widgetId) {
        if (getView() == null) return;
        CalendarSet cals = new CalendarSet(null, calendars.length);
        for (CheckBox calendar : calendars) {
            if (calendar.isChecked()) {
                cals.add((String) calendar.getTag());
            } else {
                cals.remove((String) calendar.getTag());
            }
        }
        edit.putString("cals_" + widgetId, cals.toString());
        edit.putInt("lookaheadUnit_" + widgetId,
                ((Spinner) getView().findViewById(R.id.lookaheadspinner))
                        .getSelectedItemPosition());

        try {
            edit.putInt("lookaheadtime_" + widgetId, Integer.parseInt(
                    ((EditText) getView().findViewById(R.id.lookaheadtime)).getText().toString()));
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }

        edit.putBoolean("showCalendarColors_" + widgetId, Build.VERSION.SDK_INT >= 14 &&
                ((CheckBox) getView().findViewById(R.id.colors)).isChecked());
    }

    @Override
    public void restore(final SharedPreferences prefs, final int widgetId) {
        CalendarSet cals = new CalendarSet(prefs.getString("cals_" + widgetId, null), 0);
        for (CheckBox calendar : calendars) {
            calendar.setChecked(cals.contains((String) calendar.getTag()));
        }

        ((Spinner) getView().findViewById(R.id.lookaheadspinner))
                .setSelection(prefs.getInt("lookaheadUnit_" + widgetId, 1));
        ((EditText) getView().findViewById(R.id.lookaheadtime))
                .setText(((Integer) prefs.getInt("lookaheadtime_" + widgetId, 4)).toString());


        ((CheckBox) getView().findViewById(R.id.colors))
                .setChecked(prefs.getBoolean("showCalendarColors_" + widgetId, false));

    }
}
