package de.j4velin.calendarWidget;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class Dummy extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dummy);
        findViewById(R.id.button1).setOnClickListener(v -> finish());
        getPackageManager().setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}
