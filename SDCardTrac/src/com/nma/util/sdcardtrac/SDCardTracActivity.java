package com.nma.util.sdcardtrac;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SDCardTracActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        // Start the service
        Intent serviceIntent = new Intent(this, FileObserverService.class);
        startService(serviceIntent);
    }
}
