package de.j4velin.calendarWidget;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class IcsImporter extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            BufferedReader br = null;
            InputStreamReader isr = null;
            String title = null, desc = null, location = null;
            Date startTime = new Date();
            Date endTime = new Date();
            SimpleDateFormat sdf;
            try {
                isr = new InputStreamReader(
                        getContentResolver().openInputStream(getIntent().getData()));
                br = new BufferedReader(isr);
                String line = br.readLine();
                while (line != null && !"BEGIN:VEVENT".equals(line.toUpperCase()))
                    line = br.readLine();
                while (line != null && !"END:VEVENT".equals(line.toUpperCase())) {
                    line = line.toUpperCase();
                    if (line.startsWith("SUMMARY")) {
                        title = line.split(":", 2)[1];
                    } else if (line.startsWith("DESCRIPTION")) {
                        desc = line.split(":", 2)[1];
                    } else if (line.startsWith("DTSTART")) {
                        sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                        if (line.contains("TZID")) {
                            sdf.setTimeZone(
                                    TimeZone.getTimeZone(line.split("TZID=", 2)[1].split(":")[0]));
                        } else if (line.endsWith("Z")) {
                            sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        } else {
                            sdf.setTimeZone(TimeZone.getDefault());
                        }
                        try {
                            startTime = sdf.parse(line.split(":", 2)[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if (line.startsWith("DTEND")) {
                        sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
                        if (line.contains("TZID")) {
                            sdf.setTimeZone(
                                    TimeZone.getTimeZone(line.split("TZID=", 2)[1].split(":")[0]));
                        } else if (line.endsWith("Z")) {
                            sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        } else {
                            sdf.setTimeZone(TimeZone.getDefault());
                        }
                        try {
                            endTime = sdf.parse(line.split(":", 2)[1]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else if (line.startsWith("LOCATION")) {
                        location = line.split(":", 2)[1];
                    }
                    line = br.readLine();
                }
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.getTime())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTime())
                        .putExtra(CalendarContract.Events.TITLE, title)
                        .putExtra(CalendarContract.Events.DESCRIPTION, desc)
                        .putExtra(CalendarContract.Events.EVENT_LOCATION, location);
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                if (BuildConfig.DEBUG) Logger.log(e);
                Toast.makeText(this, "No calendar app found to handle intent", Toast.LENGTH_SHORT)
                        .show();
                e.printStackTrace();
            } catch (Throwable e) {
                if (BuildConfig.DEBUG) Logger.log(e);
                Toast.makeText(this,
                        "Error: " + e.getClass().getSimpleName() + "\n" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                if (isr != null) try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (br != null) try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        finish();
    }
}
