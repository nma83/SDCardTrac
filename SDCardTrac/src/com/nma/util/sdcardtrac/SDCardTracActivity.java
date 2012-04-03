/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

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
import android.widget.TextView;
import android.widget.ToggleButton;

public class SDCardTracActivity extends Activity implements OnClickListener {
	private static final int DEFAULT_START_OFFSET_SEC = 10;
	private static final long DEFAULT_UPDATE_INTERVAL_SEC = AlarmManager.INTERVAL_HALF_DAY;
	
	// Handle to running service
	private FileObserverService.TrackingBinder serviceBind;
	// State variables
	private boolean boundToService = false;
	private boolean alarmSetupDone = false;
	private int storeStartOffset = DEFAULT_START_OFFSET_SEC;
	private long storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_SEC;
	// Alarms
	PendingIntent alarmIntent;
	
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

        // Setup UI event listeners
        ((Button)findViewById(R.id.apply_config_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.default_config_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.background_control_toggle)).setOnClickListener(this);
        ((Button)findViewById(R.id.trigger_collect_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.show_graph_button)).setOnClickListener(this);
        ((Button)findViewById(R.id.exit_app_button)).setOnClickListener(this);

        // Setup alarm intent - one time
        Intent triggerIntent = new Intent(this, DeltaCompute.class);
        alarmIntent = PendingIntent.getBroadcast(this, 0, triggerIntent, 0);

        // Bringup stored state, assigns to store*
        restoreConfig();

    	// Start monitoring service
        if (alarmSetupDone) {
        	backgroundService(true);
        	manageAlarm(true);
        }
        
        // Set default state of UI
        ((ToggleButton)findViewById(R.id.background_control_toggle)).setChecked(alarmSetupDone);
        ((TextView)findViewById(R.id.init_offset_value)).setText(Integer.toString(storeStartOffset));
		((TextView)findViewById(R.id.update_interval_value)).setText(Integer.toString((int)storeTriggerInterval));
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    }
    
    // Update persistent configuration
    @Override
    protected void onPause() {
    	super.onPause();
    	updateConfig();
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
    		storeStartOffset = Integer.parseInt(startOffsetStr);
    		storeTriggerInterval = Long.parseLong(triggerIntervalStr);
    		break;
    	case R.id.default_config_button:
    		((TextView)findViewById(R.id.init_offset_value)).setText(Integer.toString(DEFAULT_START_OFFSET_SEC));
    		((TextView)findViewById(R.id.update_interval_value)).setText(Integer.toString((int)DEFAULT_UPDATE_INTERVAL_SEC));
    		storeStartOffset = DEFAULT_START_OFFSET_SEC;
    		storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_SEC;
    		break;
    	case R.id.background_control_toggle:
    		checked = ((ToggleButton)findViewById(R.id.background_control_toggle)).isChecked();
    		backgroundService(checked);
    		manageAlarm(checked);
    		// Disable trigger button if background disabled
    		((Button)findViewById(R.id.trigger_collect_button)).setEnabled(checked);
    		break;
    	case R.id.trigger_collect_button:
    		Intent triggerCollect = new Intent(this, FileObserverService.class);
    		triggerCollect.setAction(Intent.ACTION_VIEW);
    		startService(triggerCollect);
    		break;
    	case R.id.show_graph_button:
    		Intent showGraph = new Intent(this, GraphActivity.class);
    		startActivity(showGraph);
    		break;
    	case R.id.exit_app_button:
    		finish();
    		break;
    	}
    }
    
    // Helper methods
    // Restore the state
    private void restoreConfig() {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        // Get configuration
        storeStartOffset = prefs.getInt("AlarmStartOffset", DEFAULT_START_OFFSET_SEC);
        storeTriggerInterval = prefs.getLong("AlarmTriggerInterval", DEFAULT_UPDATE_INTERVAL_SEC);
        alarmSetupDone = prefs.getBoolean("AlarmEnabled", true);
    	Log.d(getClass().getName(), "Restored: " + storeStartOffset + ", " + storeTriggerInterval + ", "
    			+ alarmSetupDone);

    }

    // Update persistent configuration
    private void updateConfig() {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	SharedPreferences.Editor prefEdit = prefs.edit();
    	prefEdit.putInt("AlarmStartOffset", storeStartOffset);
    	prefEdit.putLong("AlarmTriggerInterval", storeTriggerInterval);
    	prefEdit.putBoolean("AlarmEnabled", alarmSetupDone);
    	prefEdit.apply();
    	Log.d(getClass().getName(), "Applied: " + storeStartOffset + ", " + storeTriggerInterval + ", "
    			+ alarmSetupDone);
    }
    
    // Start the background service
    private void backgroundService(boolean enable) {
    	// Control the service
    	Intent serviceIntent = new Intent(this, FileObserverService.class);
    	
    	if (enable) {
    		serviceIntent.setAction(Intent.ACTION_MAIN);
    		startService(serviceIntent);
    		// Bind to service
    		//bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    	} else {
    		//unbindService(serviceConn);
    		//boundToService = false;
    		serviceIntent.setAction(Intent.ACTION_DELETE);
    		startService(serviceIntent);
    		// TODO: stopService(serviceIntent);
    	}
    }
    
    // Setup recurring collection of events
    private void manageAlarm(boolean enable) {
    	if (enable) {
    		if (alarmSetupDone) {
    			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
    			alarmSetupDone = false;
    		}
            // Setup the alarm
            setupEventCollection(storeStartOffset, storeTriggerInterval * 1000);
    	} else if (alarmSetupDone) {
			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
			Log.d(getClass().getName(), "Cancelling alarms");
			alarmSetupDone = false;
    	}
    }
    
    // Alarm setup
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
