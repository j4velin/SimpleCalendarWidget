package de.j4velin.calendarWidget.settings;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.j4velin.calendarWidget.R;
import de.j4velin.calendarWidget.WidgetService;

public class Fragment_Appearance extends WidgetSettingsFragment implements View.OnClickListener {

    public final static int DEFAULT_BG_COLOR = 1694498816;
    public final static int DEFAULT_DATE_COLOR = Color.WHITE;
    public final static int DEFAULT_PASSED_COLOR = Color.GRAY;
    public final static int DEFAULT_EVENT_COLOR = Color.WHITE;
    public final static int DEFAULT_TIME_COLOR = Color.WHITE;
    public final static int DEFAULT_LOCATION_COLOR = Color.WHITE;

    private final List<CompoundButton> compoundButtons = new ArrayList<>(17);

    public final static String DEFAULT_DATE_FORMAT =
            (Locale.getDefault().getDisplayLanguage().equals(Locale.ENGLISH.getDisplayLanguage())) ?
                    "EEEE, MMM/dd" : "EEEE, dd. MMM";
    public final static String DEFAULT_TODAY_DATE_FORMAT =
            (Locale.getDefault().getDisplayLanguage().equals(Locale.ENGLISH.getDisplayLanguage())) ?
                    "EEEE, MMMM/dd" : "EEEE, dd. MMMM";

    public static String getDefaultTimeFormat(final Context c) {
        return DateFormat.is24HourFormat(c) ? "HH:mm" : "hh:mm a";
    }

    public final static int DEFAULT_DATE_SIZE = 14;
    public final static int DEFAULT_EVENT_SIZE = 12;
    public final static int DEFAULT_TIME_SIZE = 12;
    public final static int DEFAULT_TODAY_SIZE = 20;
    public final static int DEFAULT_LOCATION_SIZE = 10;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.config_appearance, container, false);

        v.findViewById(R.id.currentDateColor).setOnClickListener(this);
        v.findViewById(R.id.dateColor).setOnClickListener(this);
        v.findViewById(R.id.timeColor).setOnClickListener(this);
        v.findViewById(R.id.eventColor).setOnClickListener(this);
        v.findViewById(R.id.locationColor).setOnClickListener(this);
        v.findViewById(R.id.todayEventColor).setOnClickListener(this);
        v.findViewById(R.id.todayPassedEventColor).setOnClickListener(this);
        v.findViewById(R.id.backgroundColor).setOnClickListener(this);

        CheckBox skipPassed = v.findViewById(R.id.skipPassed);
        final View skipLayout = v.findViewById(R.id.skipPassedLayout);
        skipPassed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                skipLayout.setVisibility(checked ? View.INVISIBLE : View.VISIBLE);
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
    public void onClick(final View v) {
        if (v instanceof ColorPreviewButton) {
            ColorPickerDialog dialog =
                    new ColorPickerDialog(getActivity(), ((ColorPreviewButton) v).getColor());
            dialog.setHexValueEnabled(true);
            dialog.setAlphaSliderVisible(v.getId() == R.id.backgroundColor);
            dialog.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                @Override
                public void onColorChanged(int color) {
                    ((ColorPreviewButton) v).setColor(color);
                    v.setTag(color);
                }
            });
            dialog.show();
        }
    }

    @Override
    public void save(final SharedPreferences.Editor edit, final int widgetId) {
        if (getView() == null) return;
        try {
            edit.putFloat("titlesize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.dateSize)).getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("textsize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.eventSize)).getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("today_textsize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.todayEventSize)).getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("today_passed_textsize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.todayPassedEventSize)).getText()
                            .toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("timesize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.timeSize)).getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("locationsize_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.locationSize)).getText().toString()));
        } catch (NumberFormatException nfe) {
        }
        try {
            edit.putFloat("current_date_size_" + widgetId, Float.parseFloat(
                    ((EditText) getView().findViewById(R.id.currentDateSize)).getText()
                            .toString()));
        } catch (NumberFormatException nfe) {
        }

        edit.putInt("text_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.eventColor)).getColor());
        edit.putInt("bg_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.backgroundColor)).getColor());
        edit.putInt("title_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.dateColor)).getColor());
        edit.putInt("time_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.timeColor)).getColor());
        edit.putInt("location_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.locationColor)).getColor());
        edit.putInt("today_text_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.todayEventColor)).getColor());
        edit.putInt("today_passed_text_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.todayPassedEventColor))
                        .getColor());
        edit.putInt("current_date_color_" + widgetId,
                ((ColorPreviewButton) getView().findViewById(R.id.currentDateColor)).getColor());


        String datef = ((EditText) getView().findViewById(R.id.dateFormat)).getText().toString();
        try {
            new SimpleDateFormat(datef).format(System.currentTimeMillis());
            edit.putString("dateformat_" + widgetId, datef);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Invalid date format: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        String timef = ((EditText) getView().findViewById(R.id.timeFormat)).getText().toString();
        try {
            new SimpleDateFormat(timef).format(System.currentTimeMillis());
            edit.putString("timeformat_" + widgetId, timef);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Invalid time format: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        CheckBox today = (CheckBox) getView().findViewById(R.id.showCurrentDate);
        if (today.isChecked()) {
            String currentf =
                    ((EditText) getView().findViewById(R.id.currentDateFormat)).getText().toString();
            try {
                new SimpleDateFormat(currentf).format(System.currentTimeMillis());
                edit.putString("today_" + widgetId, currentf);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Invalid date format: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        } else {
            edit.putString("today_" + widgetId, null);
        }

        edit.putBoolean("endTimes_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.endtimes)).isChecked());
        edit.putBoolean("showLocation_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.showLocation)).isChecked());

        edit.putBoolean("datebold_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.dateBold)).isChecked());
        edit.putBoolean("timebold_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.timeBold)).isChecked());
        edit.putBoolean("eventbold_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.eventBold)).isChecked());
        edit.putBoolean("locationbold_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.locationBold)).isChecked());
        edit.putBoolean("current_date_bold_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.currentDateBold)).isChecked());

        if (((CheckBox) getView().findViewById(R.id.dateLight)).isChecked()) {
            if (((CheckBox) getView().findViewById(R.id.eventLight)).isChecked()) {
                edit.putInt("layout_" + widgetId, WidgetService.LAYOUT_ALL_LIGHT);
            } else {
                edit.putInt("layout_" + widgetId, WidgetService.LAYOUT_TITLE_LIGHT);
            }
        } else {
            if (((CheckBox) getView().findViewById(R.id.eventLight)).isChecked()) {
                edit.putInt("layout_" + widgetId, WidgetService.LAYOUT_TEXT_LIGHT);
            } else {
                edit.putInt("layout_" + widgetId, WidgetService.LAYOUT_NONE_LIGHT);
            }
        }

        edit.putBoolean("singleLineEvent_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.singleLineEvent)).isChecked());
        edit.putBoolean("singleLineToday_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.singleLineToday)).isChecked());
        edit.putBoolean("singleLineTodayPassed_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.singleLineTodayPassed)).isChecked());
        edit.putBoolean("singleLineLocation_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.singleLineLocation)).isChecked());

        edit.putBoolean("skipPassed_" + widgetId,
                ((CheckBox) getView().findViewById(R.id.skipPassed)).isChecked());

        RadioButton sameTimeLocationAsEventToday =
                getView().findViewById(R.id.timeLocationTodaySameAsEvent);
        edit.putBoolean("timeLocationTodaySameAsEvent_" + widgetId,
                sameTimeLocationAsEventToday.isChecked());
    }

    @Override
    public void restore(final SharedPreferences prefs, final int widgetId) {
        View v = getView();

        ((ColorPreviewButton) v.findViewById(R.id.timeColor))
                .setColor(prefs.getInt("time_" + widgetId, DEFAULT_TIME_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.dateColor))
                .setColor(prefs.getInt("title_" + widgetId, DEFAULT_DATE_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.eventColor))
                .setColor(prefs.getInt("text_" + widgetId, DEFAULT_EVENT_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.locationColor))
                .setColor(prefs.getInt("location_" + widgetId, DEFAULT_LOCATION_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.backgroundColor))
                .setColor(prefs.getInt("bg_" + widgetId, DEFAULT_BG_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.todayEventColor))
                .setColor(prefs.getInt("today_text_" + widgetId, DEFAULT_EVENT_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.todayPassedEventColor))
                .setColor(prefs.getInt("today_passed_text_" + widgetId, DEFAULT_PASSED_COLOR));
        ((ColorPreviewButton) v.findViewById(R.id.currentDateColor))
                .setColor(prefs.getInt("current_date_color_" + widgetId, DEFAULT_DATE_COLOR));

        ((EditText) v.findViewById(R.id.dateSize)).setText(
                ((Float) prefs.getFloat("titlesize_" + widgetId, DEFAULT_DATE_SIZE)).toString());
        ((EditText) v.findViewById(R.id.eventSize)).setText(
                ((Float) prefs.getFloat("textsize_" + widgetId, DEFAULT_EVENT_SIZE)).toString());
        ((EditText) v.findViewById(R.id.todayEventSize)).setText(
                ((Float) prefs.getFloat("today_textsize_" + widgetId, DEFAULT_TODAY_SIZE))
                        .toString());
        ((EditText) v.findViewById(R.id.todayPassedEventSize)).setText(
                ((Float) prefs.getFloat("today_passed_textsize_" + widgetId, DEFAULT_EVENT_SIZE))
                        .toString());
        ((EditText) v.findViewById(R.id.timeSize)).setText(
                ((Float) prefs.getFloat("timesize_" + widgetId, DEFAULT_TIME_SIZE)).toString());
        ((EditText) v.findViewById(R.id.locationSize)).setText(
                ((Float) prefs.getFloat("locationsize_" + widgetId, DEFAULT_LOCATION_SIZE))
                        .toString());
        ((EditText) v.findViewById(R.id.currentDateSize)).setText(
                ((Float) prefs.getFloat("current_date_size_" + widgetId, DEFAULT_DATE_SIZE))
                        .toString());


        ((EditText) v.findViewById(R.id.dateFormat))
                .setText(prefs.getString("dateformat_" + widgetId, DEFAULT_DATE_FORMAT));
        ((EditText) v.findViewById(R.id.timeFormat)).setText(
                prefs.getString("timeformat_" + widgetId, getDefaultTimeFormat(getActivity())));

        CheckBox cbShowLocation = v.findViewById(R.id.showLocation);
        cbShowLocation.setChecked(prefs.getBoolean("showLocation_" + widgetId, false));
        CheckBox cbEndTimes = v.findViewById(R.id.endtimes);
        cbEndTimes.setChecked(prefs.getBoolean("endTimes_" + widgetId, false));
        compoundButtons.add(cbEndTimes);
        compoundButtons.add(cbShowLocation);

        CheckBox cbShowCurrentDate = v.findViewById(R.id.showCurrentDate);
        cbShowCurrentDate.setChecked(prefs.getString("today_" + widgetId, null) != null);
        compoundButtons.add(cbShowCurrentDate);
        ((EditText) v.findViewById(R.id.currentDateFormat))
                .setText(prefs.getString("today_" + widgetId, DEFAULT_TODAY_DATE_FORMAT));

        // bold
        CheckBox cbBoldDate = v.findViewById(R.id.dateBold);
        cbBoldDate.setChecked(prefs.getBoolean("datebold_" + widgetId, true));
        CheckBox cbBoldTime = v.findViewById(R.id.timeBold);
        cbBoldTime.setChecked(prefs.getBoolean("timebold_" + widgetId, false));
        CheckBox cbBoldEvent = v.findViewById(R.id.eventBold);
        cbBoldEvent.setChecked(prefs.getBoolean("eventbold_" + widgetId, false));
        CheckBox cbBoldLocation = v.findViewById(R.id.locationBold);
        cbBoldLocation.setChecked(prefs.getBoolean("locationbold_" + widgetId, false));
        CheckBox cbBoldCurrentDate = v.findViewById(R.id.currentDateBold);
        cbBoldCurrentDate.setChecked(prefs.getBoolean("current_date_bold_" + widgetId, false));
        compoundButtons.add(cbBoldDate);
        compoundButtons.add(cbBoldTime);
        compoundButtons.add(cbBoldEvent);
        compoundButtons.add(cbBoldLocation);
        compoundButtons.add(cbBoldCurrentDate);

        // light
        if (android.os.Build.VERSION.SDK_INT < 16) {
            v.findViewById(R.id.dateLight).setVisibility(View.GONE);
            v.findViewById(R.id.eventLight).setVisibility(View.GONE);
        } else {
            int layout = prefs.getInt("layout_" + widgetId, WidgetService.LAYOUT_TEXT_LIGHT);
            CheckBox cblightDate = v.findViewById(R.id.dateLight);
            cblightDate.setChecked(layout == WidgetService.LAYOUT_TITLE_LIGHT ||
                    layout == WidgetService.LAYOUT_ALL_LIGHT);
            CheckBox cblightEvent = v.findViewById(R.id.eventLight);
            cblightEvent.setChecked(layout == WidgetService.LAYOUT_TEXT_LIGHT ||
                    layout == WidgetService.LAYOUT_ALL_LIGHT);
            compoundButtons.add(cblightDate);
            compoundButtons.add(cblightEvent);
        }

        // singleline
        CheckBox cbLineEvent = v.findViewById(R.id.singleLineEvent);
        cbLineEvent.setChecked(prefs.getBoolean("singleLineEvent_" + widgetId, false));
        CheckBox cbLineToday = v.findViewById(R.id.singleLineToday);
        cbLineToday.setChecked(prefs.getBoolean("singleLineToday_" + widgetId, false));
        CheckBox cbLineTodayPassed = v.findViewById(R.id.singleLineTodayPassed);
        cbLineTodayPassed.setChecked(prefs.getBoolean("singleLineTodayPassed_" + widgetId, false));
        CheckBox cbLineLocation = v.findViewById(R.id.singleLineLocation);
        cbLineLocation.setChecked(prefs.getBoolean("singleLineLocation_" + widgetId, false));
        compoundButtons.add(cbLineEvent);
        compoundButtons.add(cbLineToday);
        compoundButtons.add(cbLineTodayPassed);
        compoundButtons.add(cbLineLocation);

        // skip passed
        CheckBox cbSkipPassed = v.findViewById(R.id.skipPassed);
        cbSkipPassed.setChecked(prefs.getBoolean("skipPassed_" + widgetId, true));
        compoundButtons.add(cbSkipPassed);

        // today same time/location as event
        RadioButton sameTimeLocationAsEventToday =
                v.findViewById(R.id.timeLocationTodaySameAsEvent);
        RadioButton sameTimeLocationAsOtherDays =
                v.findViewById(R.id.timeLocationTodaySameAsOtherDay);
        sameTimeLocationAsEventToday
                .setChecked(prefs.getBoolean("timeLocationTodaySameAsEvent_" + widgetId, true));
        sameTimeLocationAsOtherDays.setChecked(!sameTimeLocationAsEventToday.isChecked());
        compoundButtons.add(sameTimeLocationAsEventToday);
        compoundButtons.add(sameTimeLocationAsOtherDays);
    }
}
