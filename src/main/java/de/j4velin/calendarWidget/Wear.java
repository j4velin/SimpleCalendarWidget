package de.j4velin.calendarWidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.text.SimpleDateFormat;
import java.util.List;

public class Wear extends WearableListenerService {

    private final static boolean LOG = false;
    private GoogleApiClient apiClient;

    @Override
    public void onMessageReceived(@NonNull final MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (LOG) android.util.Log.d("SCW", "onMessageReceived: " + messageEvent.getPath());
        if (messageEvent.getPath().equals("/scw/getEvents")) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(final Bundle connectionHint) {
                            SharedPreferences prefs =
                                    getSharedPreferences("calendarWidget", MODE_MULTI_PROCESS);
                            int id;
                            long time = System.currentTimeMillis();
                            if (prefs.contains("lastConfiguredWidgetId")) {
                                id = prefs.getInt("lastConfiguredWidgetId", 0);
                            } else {
                                int ids[] = AppWidgetManager.getInstance(Wear.this).getAppWidgetIds(
                                        new ComponentName(Wear.this, Widget.class));
                                if (ids.length == 0) {
                                    PutDataRequest empty = PutDataRequest.create("/scw/nowidget");
                                    empty.setData(String.valueOf(time).getBytes());
                                    Wearable.DataApi.putDataItem(apiClient, empty);
                                    apiClient.disconnect();
                                    return;
                                } else {
                                    id = ids[ids.length - 1];
                                }
                            }
                            SimpleDateFormat timeFormat = new SimpleDateFormat(
                                    prefs.getString("timeformat_" + id, "HH:mm"));
                            SimpleDateFormat dateFormat = new SimpleDateFormat(
                                    prefs.getString("dateformat_" + id, "dd.MM."));
                            String[] events;
                            List<Day> days;
                            try {
                                days = Parser.parse(Wear.this, id);
                            } catch (SecurityException se) {
                                PutDataRequest empty = PutDataRequest.create("/scw/permission");
                                empty.setData(String.valueOf(time).getBytes());
                                Wearable.DataApi.putDataItem(apiClient, empty);
                                apiClient.disconnect();
                                return;
                            }
                            if (days == null || days.isEmpty()) {
                                PutDataRequest empty = PutDataRequest.create("/scw/empty");
                                empty.setData(String.valueOf(time).getBytes());
                                Wearable.DataApi.putDataItem(apiClient, empty);
                            } else {
                                if (LOG) android.util.Log.d("SCW", "days size: " + days.size());
                                for (int i = 0; i < days.size(); i++) {
                                    if (LOG) android.util.Log.d("SCW",
                                            "events for " + (dateFormat.format(days.get(i).date)) +
                                                    ": " + days.get(i).events.size());
                                    PutDataMapRequest dataMap =
                                            PutDataMapRequest.create("/scw/event/" + i);
                                    dataMap.getDataMap()
                                            .putString("date", dateFormat.format(days.get(i).date));
                                    dataMap.getDataMap().putInt("id", i);
                                    dataMap.getDataMap().putLong("timestamp", time);
                                    events = new String[days.get(i).events.size()];
                                    for (int j = 0; j < days.get(i).events.size(); j++) {
                                        if (days.get(i).events.get(j).allDay)
                                            events[j] = days.get(i).events.get(j).title;
                                        else events[j] =
                                                timeFormat.format(days.get(i).events.get(j).date) +
                                                        " " + days.get(i).events.get(j).title;
                                    }
                                    dataMap.getDataMap().putStringArray("events", events);
                                    PutDataRequest request = dataMap.asPutDataRequest();
                                    Wearable.DataApi.putDataItem(apiClient, request);
                                    if (LOG) android.util.Log.d("SCW",
                                            dateFormat.format(days.get(i).date) + " sent");
                                }
                            }
                            apiClient.disconnect();
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                        }
                    }).addApi(Wearable.API).build();
            apiClient.connect();
        }
    }
}
