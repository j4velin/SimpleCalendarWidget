package de.j4velin.calendarWidget.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.j4velin.calendarWidget.BuildConfig;
import de.j4velin.calendarWidget.Logger;
import de.j4velin.calendarWidget.R;

public class BackupToSD {

    static boolean saveSharedPreferencesToFile(final int widgetId, final Context c,
                                               final String suffix) {
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            String path = c.getExternalFilesDir(null).toString() + "/backup_" + suffix;
            output = new ObjectOutputStream(new FileOutputStream(path));
            SharedPreferences pref = c.getSharedPreferences("calendarWidget", 0);
            Map<String, ?> entries = pref.getAll();
            Map<String, Object> entriesToSave = new HashMap<String, Object>();
            for (Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();
                if (key.endsWith("_" + widgetId)) {
                    entriesToSave.put(key.substring(0, key.lastIndexOf("_") + 1), v);
                }
            }

            output.writeObject(entriesToSave);

            res = true;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Logger.log(e);
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                if (BuildConfig.DEBUG) Logger.log(ex);
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({"unchecked"})
    static boolean loadSharedPreferencesFromFile(final int widgetId, final Context c,
                                                 final String suffix) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            String path = c.getExternalFilesDir(null).toString() + "/backup_" + suffix;
            if (!new File(path).exists()) {
                path = c.getExternalFilesDir(null).toString() + "/backup";
            }
            if (!new File(path).exists()) {
                Toast.makeText(c, R.string.nobackup, Toast.LENGTH_SHORT).show();
                return false;
            }
            input = new ObjectInputStream(new FileInputStream(path));
            Editor prefEdit = c.getSharedPreferences("calendarWidget", 0).edit();
            //prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean) prefEdit.putBoolean(key + widgetId, (Boolean) v);
                else if (v instanceof Float) prefEdit.putFloat(key + widgetId, (Float) v);
                else if (v instanceof Integer) prefEdit.putInt(key + widgetId, (Integer) v);
                else if (v instanceof Long) prefEdit.putLong(key + widgetId, (Long) v);
                else if (v instanceof String) prefEdit.putString(key + widgetId, ((String) v));
            }
            prefEdit.apply();
            res = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }
}
