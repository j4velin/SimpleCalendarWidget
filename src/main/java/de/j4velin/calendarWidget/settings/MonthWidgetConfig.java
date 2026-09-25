package de.j4velin.calendarWidget.settings;

import static de.j4velin.calendarWidget.settings.WidgetConfig.PERMISSION_CALENDAR;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.PermissionChecker;

import java.util.Locale;

import de.j4velin.calendarWidget.BuildConfig;
import de.j4velin.calendarWidget.Logger;
import de.j4velin.calendarWidget.MonthWidget;
import de.j4velin.calendarWidget.R;
import de.j4velin.lib.colorpicker.ColorPickerDialog;
import de.j4velin.lib.colorpicker.ColorPreviewButton;

public class MonthWidgetConfig extends Activity implements View.OnClickListener {

    public static int widgetId;
    private boolean permissionRequestRunning = false;
    private CheckBox[] calendars = new CheckBox[0];

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            setContentView(R.layout.config_month);
            if (Build.VERSION.SDK_INT >= 23 && PermissionChecker
                    .checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                    PackageManager.PERMISSION_DENIED) {
                permissionRequestRunning = true;
                requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                        PERMISSION_CALENDAR);
            } else {
                addCalendars();
            }
            final ArrayAdapter<String> adapter2 =
                    new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                            new String[]{"black", "white"});
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ((Spinner) findViewById(R.id.iconsColor)).setAdapter(adapter2);
            final ImageView iconpreview = (ImageView) findViewById(R.id.iconpreview);
            ((SeekBar) findViewById(R.id.alpha))
                    .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onStopTrackingTouch(final SeekBar seekBar) {

                        }

                        @Override
                        public void onStartTrackingTouch(final SeekBar seekBar) {

                        }

                        @Override
                        public void onProgressChanged(final SeekBar seekBar, int progress,
                                                      boolean fromUser) {
                            iconpreview.setAlpha(255 - progress);
                        }
                    });
            if (extras.containsKey("editId")) {
                widgetId = extras.getInt("editId");
            } else {
                widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }
            findViewById(R.id.currentDateTextColor).setOnClickListener(this);
            findViewById(R.id.currentDateBackgroundColor).setOnClickListener(this);
            findViewById(R.id.currentMonthTextColor).setOnClickListener(this);
            findViewById(R.id.currentMonthBackgroundColor).setOnClickListener(this);
            findViewById(R.id.otherDaysTextColor).setOnClickListener(this);
            findViewById(R.id.otherDaysBackgroundColor).setOnClickListener(this);
            findViewById(R.id.weekDayLabelTextColor).setOnClickListener(this);
            findViewById(R.id.monthLabelTextColor).setOnClickListener(this);
            findViewById(R.id.backgroundColor).setOnClickListener(this);
            restore();
            final Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, resultValue);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    private void addCalendars() {
        final Cursor cursor;
        if (Build.VERSION.SDK_INT >= 23 &&
                PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                        PackageManager.PERMISSION_DENIED) {
            permissionRequestRunning = true;
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSION_CALENDAR);
            cursor = null;
        } else {
            cursor = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                    new String[]{CalendarContract.Calendars._ID,
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                            CalendarContract.Calendars.CALENDAR_COLOR}, null, null, null);
        }
        final LinearLayout cal = (LinearLayout) findViewById(R.id.cals);
        cal.removeAllViews();
        if (cursor != null && cursor.getCount() > 0) {
            calendars = new CheckBox[cursor.getCount()];
            cursor.moveToFirst();
            LinearLayout row;
            CheckBox box;
            View color;
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(20, LinearLayout.LayoutParams.MATCH_PARENT);
            int pos = 0;
            do {
                row = new LinearLayout(this);
                box = new CheckBox(this);
                if (android.os.Build.VERSION.SDK_INT >= 14) {
                    color = new View(this);
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
            TextView tv = new TextView(this);
            tv.setText("No calendars found");
            cal.addView(tv);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions,
                                           final int[] grantResults) {
        permissionRequestRunning = false;
        if (requestCode == WidgetConfig.PERMISSION_EXTERNAL_STORAGE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showBackupDialog();
            }
        } else if (requestCode == WidgetConfig.PERMISSION_CALENDAR && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addCalendars();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showBackupDialog() {
        if (Build.VERSION.SDK_INT >= 23 && PermissionChecker
                .checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED) {
            permissionRequestRunning = true;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WidgetConfig.PERMISSION_EXTERNAL_STORAGE);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.backupandrestore).setMessage(R.string.backuplocation)
                .setPositiveButton("Backup", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        try {
                            save();
                            BackupToSD.saveSharedPreferencesToFile(widgetId, MonthWidgetConfig.this,
                                    "month");
                            if (BuildConfig.DEBUG) Logger.log("backup saved");
                            Toast.makeText(MonthWidgetConfig.this,
                                    getString(R.string.backupsaved) + " " +
                                            getExternalFilesDir(null).toString() + "/backup_month",
                                    Toast.LENGTH_LONG).show();
                        } catch (NullPointerException npe) {
                            if (BuildConfig.DEBUG) Logger.log(npe);
                            npe.printStackTrace();
                            Toast.makeText(MonthWidgetConfig.this,
                                    getString(R.string.externalstorageerror), Toast.LENGTH_LONG)
                                    .show();
                        } catch (OutOfMemoryError e) {
                            if (BuildConfig.DEBUG) Logger.log(e);
                            Toast.makeText(MonthWidgetConfig.this,
                                    "Error: Run out of memory when saving backup",
                                    Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                }).setNeutralButton(getString(R.string.restore),
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        try {
                            BackupToSD
                                    .loadSharedPreferencesFromFile(widgetId, MonthWidgetConfig.this,
                                            "month");
                            restore();
                            Toast.makeText(MonthWidgetConfig.this, R.string.restored,
                                    Toast.LENGTH_SHORT).show();
                        } catch (OutOfMemoryError e) {
                            Toast.makeText(MonthWidgetConfig.this,
                                    "Error: Run out of memory when loading backup",
                                    Toast.LENGTH_LONG).show();
                        } catch (NullPointerException npe) {
                            npe.printStackTrace();
                            Toast.makeText(MonthWidgetConfig.this, R.string.externalstorageerror,
                                    Toast.LENGTH_LONG).show();
                        }
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.website:
                startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse("https://j4velin.de/contact.php"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.apps:
                startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:j4velin"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.backup:
                showBackupDialog();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.days);
        appWidgetManager.updateAppWidget(widgetId,
                MonthWidget.updateWidget(widgetId, MonthWidgetConfig.this));
        if (!permissionRequestRunning) finish();
    }

    private void restore() {
        final SharedPreferences prefs =
                getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        CalendarSet cals = new CalendarSet(prefs.getString("cals_" + widgetId, null), 0);
        for (CheckBox calendar : calendars) {
            calendar.setChecked(cals.contains((String) calendar.getTag()));
        }
        ((ColorPreviewButton) findViewById(R.id.currentDateTextColor))
                .setColor(prefs.getInt("today_text_" + widgetId, Color.BLACK));
        ((ColorPreviewButton) findViewById(R.id.currentDateBackgroundColor))
                .setColor(prefs.getInt("today_bg_" + widgetId, Color.WHITE));
        ((ColorPreviewButton) findViewById(R.id.backgroundColor))
                .setColor(prefs.getInt("monthwidget_bg_" + widgetId, Color.TRANSPARENT));
        ((ColorPreviewButton) findViewById(R.id.currentMonthTextColor))
                .setColor(prefs.getInt("month_text_" + widgetId, Color.WHITE));
        ((ColorPreviewButton) findViewById(R.id.currentMonthBackgroundColor))
                .setColor(prefs.getInt("month_bg_" + widgetId, Color.argb(75, 255, 255, 255)));
        ((ColorPreviewButton) findViewById(R.id.otherDaysTextColor))
                .setColor(prefs.getInt("other_text_" + widgetId, Color.LTGRAY));
        ((ColorPreviewButton) findViewById(R.id.otherDaysBackgroundColor))
                .setColor(prefs.getInt("other_bg_" + widgetId, Color.TRANSPARENT));
        ((ColorPreviewButton) findViewById(R.id.weekDayLabelTextColor))
                .setColor(prefs.getInt("dayslabel_text_" + widgetId, Color.LTGRAY));
        ((ColorPreviewButton) findViewById(R.id.monthLabelTextColor))
                .setColor(prefs.getInt("monthlabel_text_" + widgetId, Color.WHITE));
        ((CheckBox) findViewById(R.id.startmonday)).setChecked(
                prefs.getBoolean("start_monday_" + widgetId, !Locale.getDefault().getCountry()
                        .equalsIgnoreCase(Locale.US.getCountry())));

        ((SeekBar) findViewById(R.id.alpha))
                .setProgress(255 - prefs.getInt("icon_alpha_" + widgetId, 255));
        ((ImageView) findViewById(R.id.iconpreview))
                .setAlpha(prefs.getInt("icon_alpha_" + widgetId, 255));
        ((Spinner) findViewById(R.id.iconsColor))
                .setSelection(prefs.getInt("icon_" + widgetId, 2) - 1);
    }

    private void save() {
        final SharedPreferences prefs =
                getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        final Editor edit = prefs.edit();

        CalendarSet cals = new CalendarSet(null, calendars.length);
        for (CheckBox calendar : calendars) {
            if (calendar.isChecked()) {
                cals.add((String) calendar.getTag());
            } else {
                cals.remove((String) calendar.getTag());
            }
        }
        edit.putString("cals_" + widgetId, cals.toString());

        edit.putInt("month_offset_" + widgetId, 0);
        edit.putInt("monthwidget_bg_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.backgroundColor)).getColor());

        edit.putInt("today_text_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.currentDateTextColor)).getColor());
        edit.putInt("today_bg_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.currentDateBackgroundColor)).getColor());

        edit.putInt("month_text_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.currentMonthTextColor)).getColor());
        edit.putInt("month_bg_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.currentMonthBackgroundColor)).getColor());

        edit.putInt("other_text_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.otherDaysTextColor)).getColor());
        edit.putInt("other_bg_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.otherDaysBackgroundColor)).getColor());

        edit.putInt("dayslabel_text_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.weekDayLabelTextColor)).getColor());
        edit.putInt("monthlabel_text_" + widgetId,
                ((ColorPreviewButton) findViewById(R.id.monthLabelTextColor)).getColor());

        edit.putBoolean("start_monday_" + widgetId,
                ((CheckBox) findViewById(R.id.startmonday)).isChecked());

        // 1 -> black
        // 2 -> white
        edit.putInt("icon_" + widgetId,
                ((Spinner) findViewById(R.id.iconsColor)).getSelectedItem().toString()
                        .equals("black") ? 1 : 2);

        edit.putInt("icon_alpha_" + widgetId,
                255 - ((SeekBar) findViewById(R.id.alpha)).getProgress());

        edit.apply();
        if (BuildConfig.DEBUG) Logger.log("settings saved for widget " + widgetId);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.currentDateTextColor:
            case R.id.currentDateBackgroundColor:
            case R.id.currentMonthTextColor:
            case R.id.currentMonthBackgroundColor:
            case R.id.otherDaysTextColor:
            case R.id.otherDaysBackgroundColor:
            case R.id.weekDayLabelTextColor:
            case R.id.monthLabelTextColor:
            case R.id.backgroundColor:
                ColorPickerDialog dialog = new ColorPickerDialog(this,
                        ((ColorPreviewButton) v).getColor());
                dialog.setHexValueEnabled(true);
                dialog.setAlphaSliderVisible(true);
                dialog.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int color) {
                        ((ColorPreviewButton) v).setColor(color);
                    }
                });
                dialog.show();
                break;
        }
    }
}
