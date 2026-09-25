package de.j4velin.calendarWidget.settings;


import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;

public abstract class WidgetSettingsFragment extends Fragment {
    public abstract void save(final SharedPreferences.Editor edit, final int widgetId);

    public abstract void restore(final SharedPreferences prefs, final int widgetId);
}


