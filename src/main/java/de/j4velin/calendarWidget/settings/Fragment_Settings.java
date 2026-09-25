package de.j4velin.calendarWidget.settings;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.j4velin.calendarWidget.R;

public class Fragment_Settings extends WidgetSettingsFragment {

    public final static String DEFAULT_ALLDAY_END_FORMAT =
            (Locale.getDefault().getDisplayLanguage().equals(Locale.ENGLISH.getDisplayLanguage())) ?
                    "MM/dd" : "dd.MM.";
    private final List<CompoundButton> compoundButtons = new ArrayList<>(7);

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.config_settings, container, false);
        v.findViewById(R.id.otherapp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new AppPicker().execute();
            }
        });
        v.findViewById(R.id.allday_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (((RadioButton) view).isChecked()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final EditText text = new EditText(getActivity());
                    String current = (String) view.getTag();
                    if (current.length() < 1) current = DEFAULT_ALLDAY_END_FORMAT;
                    text.setText(current);
                    text.setSelectAllOnFocus(true);
                    builder.setView(text);
                    builder.setMessage("Date format:").setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try {
                                        new SimpleDateFormat(text.getText().toString())
                                                .format(System.currentTimeMillis());
                                        view.setTag(text.getText().toString());
                                        dialog.dismiss();
                                    } catch (Exception e) {
                                        Toast.makeText(getActivity(),
                                                "Invalid date format: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            ((RadioButton) getView().findViewById(R.id.allday_every))
                                    .setChecked(true);
                            ((RadioButton) view).setChecked(false);
                            dialog.cancel();
                        }
                    });
                    builder.create().show();
                }
            }
        });

        final View onlyOneEventOpen = v.findViewById(R.id.openevent);
        RadioGroup rg = v.findViewById(R.id.clickgroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                onlyOneEventOpen
                        .setVisibility(checkedId == R.id.update ? View.INVISIBLE : View.VISIBLE);
            }
        });

        final ArrayAdapter<String> adapter2 =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item,
                        new String[]{"black", "white"});
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) v.findViewById(R.id.iconsColor)).setAdapter(adapter2);
        final ImageView iconpreview = (ImageView) v.findViewById(R.id.iconpreview);
        ((SeekBar) v.findViewById(R.id.alpha))
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

        return v;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        restore(getActivity().getSharedPreferences("calendarWidget", Context.MODE_PRIVATE),
                WidgetConfig.widgetId);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Util.updateCOmpoundButtonsView(compoundButtons);
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
        RadioButton otherApp = getView().findViewById(R.id.otherapp);
        RadioButton defaultApp = getView().findViewById(R.id.opendefault);
        if (otherApp.isChecked() && otherApp.getTag() != null) {
            edit.putString("otherApp_" + widgetId, (String) otherApp.getTag());
            edit.putBoolean("openCalendar_" + widgetId, false);
            edit.putBoolean("update_" + widgetId, false);
        } else if (defaultApp.isChecked()) {
            edit.remove("otherApp_" + widgetId);
            edit.putBoolean("openCalendar_" + widgetId, true);
            edit.putBoolean("update_" + widgetId, false);
        } else {
            edit.putBoolean("update_" + widgetId, true);
            edit.putBoolean("openCalendar_" + widgetId, false);
            edit.remove("otherApp_" + widgetId);
        }

        edit.putBoolean("todayText_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.todaytext)).isChecked());
        edit.putBoolean("openEvent_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.openevent)).isChecked());
        RadioButton allday_end = getView().findViewById(R.id.allday_end);
        if (allday_end.isChecked()) {
            edit.putString("allday_endformat_" + widgetId, (String) allday_end.getTag());
        } else {
            edit.putString("allday_endformat_" + widgetId, "");
        }

        // 1 -> black
        // 2 -> white
        edit.putInt("icon_" + widgetId,
                ((Spinner) getView().findViewById(R.id.iconsColor)).getSelectedItem().toString()
                        .equals("black") ? 1 : 2);

        edit.putInt("icon_alpha_" + widgetId,
                255 - ((SeekBar) getView().findViewById(R.id.alpha)).getProgress());
    }

    @Override
    public void restore(final SharedPreferences prefs, final int widgetId) {
        CheckBox todayText = getView().findViewById(R.id.todaytext);
        todayText.setChecked(prefs.getBoolean("todayText_" + widgetId, true));
        CheckBox openevent = getView().findViewById(R.id.openevent);
        openevent.setChecked(prefs.getBoolean("openEvent_" + widgetId, false));
        compoundButtons.add(todayText);
        compoundButtons.add(openevent);

        RadioButton allday_end = getView().findViewById(R.id.allday_end);
        allday_end.setChecked(prefs.getString("allday_endformat_" + widgetId, "").length() > 0);
        allday_end
                .setTag(prefs.getString("allday_endformat_" + widgetId, DEFAULT_ALLDAY_END_FORMAT));
        compoundButtons.add(allday_end);

        RadioButton allday_every = getView().findViewById(R.id.allday_every);
        allday_every.setChecked(!allday_end.isChecked());
        compoundButtons.add(allday_every);

        RadioButton opendefault = getView().findViewById(R.id.opendefault);
        RadioButton otherapp = getView().findViewById(R.id.otherapp);
        RadioButton update = getView().findViewById(R.id.update);
        if (prefs.getBoolean("openCalendar_" + widgetId, true)) {
            opendefault.setChecked(true);
        } else if (prefs.contains("otherApp_" + widgetId)) {
            otherapp.setChecked(true);
            otherapp.setTag(prefs.getString("otherApp_" + widgetId, null));
        } else {
            update.setChecked(true);
            openevent.setVisibility(View.INVISIBLE);
        }
        compoundButtons.add(opendefault);
        compoundButtons.add(otherapp);
        compoundButtons.add(update);

        ((SeekBar) getView().findViewById(R.id.alpha))
                .setProgress(255 - prefs.getInt("icon_alpha_" + widgetId, 255));
        ((ImageView) getView().findViewById(R.id.iconpreview))
                .setAlpha(prefs.getInt("icon_alpha_" + widgetId, 255));

        ((Spinner) getView().findViewById(R.id.iconsColor))
                .setSelection(prefs.getInt("icon_" + widgetId, 2) - 1);
    }


    private final class AppPicker extends AsyncTask<Void, Integer, List<TextView>> {

        private final PackageManager pm = getActivity().getPackageManager();
        private ProgressDialog progress;
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(getActivity(), "", "Loading apps", true);
            progress.setMax(3);
            progress.setCancelable(false);
        }

        protected void onPostExecute(final List<TextView> rows) {
            try {
                progress.dismiss();
            } catch (Exception e) { // activity already closed?
                e.printStackTrace();
            }
            if (rows == null) {
                Toast.makeText(getActivity(), "Error loading apps", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getActivity() != null) {
                dialog = new Dialog(getActivity());
                dialog.setTitle("Select app");
                dialog.setContentView(R.layout.apppicker_dialog);
                final LinearLayout table = (LinearLayout) dialog.findViewById(R.id.table);
                for (final TextView row : rows) {
                    if (row == null) {
                        continue;
                    }
                    table.addView(row);
                }
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((RadioButton) getView().findViewById(R.id.opendefault)).setChecked(true);
                    }
                });
                dialog.show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == 3) {
                if (progress != null && progress.isShowing()) {
                    try {
                        progress.dismiss();
                    } catch (Exception e) { // activity already closed?
                        e.printStackTrace();
                    }
                }
                Toast.makeText(getActivity(), "Not enough available memory to load all apps",
                        Toast.LENGTH_SHORT).show();
            } else {
                progress.setProgress(values[0]);
                switch (values[0]) {
                    case 1:
                        progress.setMessage("Sorting");
                        break;
                    case 2:
                        progress.setMessage("Loading icons");
                        break;
                }
            }
        }

        @Override
        protected List<TextView> doInBackground(Void... params) {
            final List<ResolveInfo> apps = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            final List<TextView> rows = new ArrayList<TextView>(apps.size());
            Context c = getActivity();
            if (c == null) return rows;
            publishProgress(1);
            try {
                Comparator<ResolveInfo> comp = new Comparator<ResolveInfo>() {
                    @Override
                    public int compare(final ResolveInfo app1, final ResolveInfo app2) {
                        try {
                            return app1.loadLabel(pm).toString().toLowerCase()
                                    .compareTo(app2.loadLabel(pm).toString().toLowerCase());
                        } catch (NullPointerException npe) {
                            return 0;
                        }
                    }
                };

                try {
                    Collections.sort(apps, comp);
                } catch (Exception iae) {
                }

                TextView name;
                Drawable icon;
                publishProgress(2);
                for (final ResolveInfo app : apps) {
                    name = new TextView(c);
                    try {
                        name.setText(app.loadLabel(pm));
                    } catch (Exception e1) {
                        name.setText("Unknown app");
                    }
                    name.setTextSize(20);
                    icon = app.loadIcon(pm);
                    icon.setBounds(0, 0, 50, 50);
                    name.setCompoundDrawablePadding(10);
                    name.setCompoundDrawables(icon, null, null, null);
                    name.setPadding(10, 10, 0, 10);
                    name.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            Intent intent = new Intent()
                                    .setClassName(app.activityInfo.packageName, // package
                                            app.activityInfo.name);
                            getView().findViewById(R.id.otherapp).setTag(intent.toUri(0));
                            dialog.dismiss();
                        }
                    });
                    rows.add(name);
                }
            } catch (OutOfMemoryError oom) {
                publishProgress(3);
            }
            return rows;
        }
    }
}
