package de.j4velin.calendarWidget.settings;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import de.j4velin.calendarWidget.BuildConfig;
import de.j4velin.calendarWidget.Logger;
import de.j4velin.calendarWidget.R;
import de.j4velin.calendarWidget.Widget;

public class WidgetConfig extends FragmentActivity {

    public final static int PERMISSION_CALENDAR = 2;

    public static int widgetId;
    private boolean permissionRequestRunning = false;

    private static SwipeableViewPager mViewPager;
    private static final int[] TABS =
            new int[]{R.string.events, R.string.appearance, R.string.settings};

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 23 &&
                PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                        PermissionChecker.PERMISSION_DENIED) {
            permissionRequestRunning = true;
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSION_CALENDAR);
        } else {
            init();
        }
    }

    private void init() {
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("editId")) {
                widgetId = extras.getInt("editId");
            } else { // default werte
                widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            setContentView(R.layout.config_tabs);

            final PageAdapter mPageAdapter = new PageAdapter(getSupportFragmentManager());
            mViewPager = findViewById(R.id.viewpager);
            mViewPager.setAdapter(mPageAdapter);

            final ActionBar actionBar = getActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            ActionBar.TabListener tabListener = new ActionBar.TabListener() {
                public void onTabSelected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                    mViewPager.setCurrentItem(tab.getPosition());
                }

                public void onTabUnselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                    // hide the given tab
                }

                public void onTabReselected(ActionBar.Tab tab, android.app.FragmentTransaction ft) {
                    // probably ignore this event
                }
            };
            actionBar.addTab(actionBar.newTab().setText(getString(TABS[0]))
                    .setTabListener(tabListener));
            actionBar.addTab(actionBar.newTab().setText(getString(TABS[1]))
                    .setTabListener(tabListener));
            actionBar.addTab(actionBar.newTab().setText(getString(TABS[2]))
                    .setTabListener(tabListener));

            mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    actionBar.setSelectedNavigationItem(position);
                }
            });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        permissionRequestRunning = false;
        if (requestCode == PERMISSION_CALENDAR && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.backupandrestore).setMessage(R.string.backuplocation)
                .setPositiveButton("Backup", (dialog, id) -> {
                    try {
                        save();
                        BackupToSD.saveSharedPreferencesToFile(widgetId, WidgetConfig.this,
                                "agenda");
                        if (BuildConfig.DEBUG) Logger.log("backup saved");
                        Toast.makeText(WidgetConfig.this,
                                getString(R.string.backupsaved) + " " +
                                        getExternalFilesDir(null).toString() + "/backup_agenda",
                                Toast.LENGTH_LONG).show();
                    } catch (NullPointerException npe) {
                        if (BuildConfig.DEBUG) Logger.log(npe);
                        Toast.makeText(WidgetConfig.this,
                                        getString(R.string.externalstorageerror), Toast.LENGTH_LONG)
                                .show();
                    } catch (OutOfMemoryError e) {
                        if (BuildConfig.DEBUG) Logger.log(e);
                        Toast.makeText(WidgetConfig.this,
                                "Error: Run out of memory when saving backup",
                                Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                }).setNeutralButton(getString(R.string.restore),
                        (dialog, id) -> {
                            try {
                                BackupToSD.loadSharedPreferencesFromFile(widgetId, WidgetConfig.this,
                                        "agenda");
                                restoreSettings();
                                Toast.makeText(WidgetConfig.this, R.string.restored, Toast.LENGTH_SHORT)
                                        .show();
                            } catch (OutOfMemoryError e) {
                                Toast.makeText(WidgetConfig.this,
                                        "Error: Run out of memory when loading backup",
                                        Toast.LENGTH_LONG).show();
                            } catch (NullPointerException npe) {
                                if (BuildConfig.DEBUG) Logger.log(npe);
                                Toast.makeText(WidgetConfig.this, R.string.externalstorageerror,
                                        Toast.LENGTH_LONG).show();
                            }
                            dialog.dismiss();
                        });
        builder.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.website) {
            startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse("https://j4velin.de/contact.php"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (id == R.id.apps) {
            startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:j4velin"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (id == R.id.backup) {
            showBackupDialog();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.events);
        appWidgetManager
                .updateAppWidget(widgetId, Widget.updateWidget(widgetId, WidgetConfig.this));
        if (!permissionRequestRunning) finish();
    }

    private void restoreSettings() {
        SharedPreferences prefs = getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        for (WidgetSettingsFragment f : PageAdapter.fragments) {
            if (f != null && f.getView() != null) f.restore(prefs, widgetId);
        }
    }

    private void save() {
        final SharedPreferences prefs =
                getSharedPreferences("calendarWidget", Context.MODE_PRIVATE);
        final Editor edit = prefs.edit();

        for (WidgetSettingsFragment f : PageAdapter.fragments) {
            if (f != null) f.save(edit, widgetId);
        }

        edit.putInt("lastConfiguredWidgetId", widgetId);

        edit.apply();
        if (BuildConfig.DEBUG) Logger.log("settings saved for widget " + widgetId);
    }

}
