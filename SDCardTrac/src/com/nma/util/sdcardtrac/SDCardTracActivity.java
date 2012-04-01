package com.nma.util.sdcardtrac;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SDCardTracActivity extends Activity implements OnClickListener {
	private static final int DEFAULT_START_OFFSET_SEC = 10;
	private static final long DEFAULT_UPDATE_INTERVAL_SEC = AlarmManager.INTERVAL_HALF_DAY;
	
	// Handle to running service
	private FileObserverService.TrackingBinder serviceBind;
	boolean boundToService = false;
	boolean alarmSetupDone = false;
	PendingIntent alarmIntent;
	// Handle to database
	private DatabaseManager trackingDB;
	
	// Connection to service
	private ServiceConnection serviceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBind = (FileObserverService.TrackingBinder)service;
			boundToService = true;
		}
		@Override
		public void onServiceDisconnected(ComponentName className) {
			boundToService = false;
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Setup alarm intent - one time
        Intent triggerIntent = new Intent(this, DeltaCompute.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, triggerIntent, 0);
        if (!alarmSetupDone) manageAlarm(true);
        // Setup the database
        trackingDB = new DatabaseManager(this);
        
        // Setup UI event listeners
        ((Button)findViewById(R.id.apply_config_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.default_config_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.background_control_toggle)).setOnClickListener(this);
        ((Button)findViewById(R.id.trigger_collect_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.show_graph_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.exit_app_button)).setOnClickListener(this);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	// Start monitoring service
    	backgroundService(true);
        // Set default state of toggle
        ((ToggleButton)findViewById(R.id.background_control_toggle)).setChecked(alarmSetupDone);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if (boundToService) {
    		unbindService(serviceConn);
    		boundToService = false;
    	}
    }
    
    // UI interaction, button click handler
    @Override
    public void onClick(View v) {
    	int currId = v.getId();
    	String startOffsetStr, triggerIntervalStr;
    	boolean checked;
    	
    	switch (currId) {
    	case R.id.apply_config_button:
    		startOffsetStr = ((TextView)findViewById(R.id.init_offset_value)).getText().toString();
    		triggerIntervalStr = ((TextView)findViewById(R.id.update_interval_value)).getText().toString();
    		checked = ((ToggleButton)findViewById(R.id.background_control_toggle)).isChecked();
    		updateConfig(Integer.parseInt(startOffsetStr, 10), Long.parseLong(triggerIntervalStr), checked);
    		break;
    	case R.id.default_config_button:
    		((TextView)findViewById(R.id.init_offset_value)).setText(DEFAULT_START_OFFSET_SEC);
    		((TextView)findViewById(R.id.update_interval_value)).setText((int)DEFAULT_UPDATE_INTERVAL_SEC);
    		break;
    	case R.id.background_control_toggle:
    		startOffsetStr = ((TextView)findViewById(R.id.init_offset_value)).getText().toString();
    		triggerIntervalStr = ((TextView)findViewById(R.id.update_interval_value)).getText().toString();
    		checked = ((ToggleButton)findViewById(R.id.background_control_toggle)).isChecked();
    		backgroundService(checked);
    		manageAlarm(checked);
    		updateConfig(Integer.parseInt(startOffsetStr, 10), Long.parseLong(triggerIntervalStr), checked);
    		// Disable trigger button if background disabled
    		((Button)findViewById(R.id.trigger_collect_button)).setEnabled(checked);
    		break;
    	case R.id.trigger_collect_button:
    		Intent triggerCollect = new Intent(this, FileObserverService.class);
    		triggerCollect.setAction(Intent.ACTION_VIEW);
    		startService(triggerCollect);
    		break;
    	case R.id.exit_app_button:
    		finish();
    		break;
    	}
    }
    
    // Helper methods
    // Start the background service
    private void backgroundService(boolean enable) {
    	// Control the service
    	Intent serviceIntent = new Intent(this, FileObserverService.class);
    	serviceIntent.setAction(Intent.ACTION_MAIN);
    	if (enable) {
    		startService(serviceIntent);
    		// Bind to service
    		bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    	} else {
    		stopService(serviceIntent);
    	}
    }

    // Update persistent configuration
    private void updateConfig(int startOffset, long updateInterval, boolean alarmEnabled) {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	SharedPreferences.Editor prefEdit = prefs.edit();
    	prefEdit.putInt("AlarmStartOffset", startOffset);
    	prefEdit.putLong("AlarmTriggerInterval", updateInterval);
    	prefEdit.putBoolean("AlarmEnabled", alarmEnabled);
    	prefEdit.apply();
    }
    
    // Setup recurring collection of events
    private void manageAlarm(boolean enable) {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	if (enable && prefs.getBoolean("AlarmEnabled", true)) {
    		if (alarmSetupDone) {
    			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
    			alarmSetupDone = false;
    		}
            // Get configuration
            int startOffset = prefs.getInt("AlarmStartOffset", DEFAULT_START_OFFSET_SEC);
            long triggerInterval = prefs.getLong("AlarmTriggerInterval", DEFAULT_UPDATE_INTERVAL_SEC);
            ((EditText)findViewById(R.id.init_offset_value)).setText(Integer.toString(startOffset));
            ((EditText)findViewById(R.id.update_interval_value)).setText(Long.toString(triggerInterval));
            
            // Setup the alarm
            setupEventCollection(startOffset, triggerInterval * 1000);
    	} else if (!alarmSetupDone) {
			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
			Log.d(getClass().getName(), "Cancelling alarms");
    	}
    }
    
    public void setupEventCollection(int startOffset, long triggerInterval) {
    	Log.d(this.getClass().getName(), "Setting up alarm for collection");
    	AlarmManager alarmEr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	Calendar time = Calendar.getInstance();
    	time.setTimeInMillis(System.currentTimeMillis());
    	time.add(Calendar.SECOND, startOffset);
    	alarmEr.setInexactRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), 
    			triggerInterval, alarmIntent); //AlarmManager.INTERVAL_FIFTEEN_MINUTES
    	Log.d(this.getClass().getName(), "Done with alarm setup");
    	alarmSetupDone = true;
    }
}
