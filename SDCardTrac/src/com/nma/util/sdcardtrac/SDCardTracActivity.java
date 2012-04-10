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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SDCardTracActivity extends Activity 
	implements OnClickListener, OnItemSelectedListener {
	private static final int DEFAULT_START_OFFSET_SEC = 10;
	private static final long DEFAULT_UPDATE_INTERVAL_SEC = AlarmManager.INTERVAL_HALF_DAY;
	
	// Handle to running service
	private FileObserverService.TrackingBinder serviceBind;
	// State variables
	private boolean boundToService = false;
	private boolean alarmSetupDone = false;
	private int storeStartOffset = DEFAULT_START_OFFSET_SEC;
	private long storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_SEC;
	private int storeIntervalIndex = 0;
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
        if (!alarmSetupDone) {
        	backgroundService(true);
        	manageAlarm(true);
        }
        
        // Set default state of UI
        ((ToggleButton)findViewById(R.id.background_control_toggle)).setChecked(alarmSetupDone);
        ((Button)findViewById(R.id.trigger_collect_button)).setEnabled(alarmSetupDone);
        ((TextView)findViewById(R.id.init_offset_value)).setText(Integer.toString(storeStartOffset));
		((TextView)findViewById(R.id.update_interval_value)).setText(Integer.toString((int)storeTriggerInterval));
		if (storeIntervalIndex != 5) {
			((TextView)findViewById(R.id.update_interval_value)).setEnabled(false);
		}
		// Setup the selector
        Spinner intervalSel = (Spinner)findViewById(R.id.update_interval_select);
        ArrayAdapter <CharSequence> defIntervals = ArrayAdapter.createFromResource(this,
        		R.array.update_intervals_phase, android.R.layout.simple_spinner_item);
        defIntervals.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSel.setAdapter(defIntervals);
        intervalSel.setOnItemSelectedListener(this);
		intervalSel.setSelection(storeIntervalIndex, true);
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
    		if (storeIntervalIndex == 5) {
    			storeTriggerInterval = Long.parseLong(triggerIntervalStr);
    		} else { // Get from spinner
    			switch (storeIntervalIndex) {
    			case 0: storeTriggerInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES; break;
    			case 1: storeTriggerInterval = AlarmManager.INTERVAL_HALF_HOUR; break;
    			case 2: storeTriggerInterval = AlarmManager.INTERVAL_HOUR; break;
    			case 3: storeTriggerInterval = AlarmManager.INTERVAL_HALF_DAY; break;
    			case 4: storeTriggerInterval = AlarmManager.INTERVAL_DAY; break;
    			}
    			((TextView)findViewById(R.id.update_interval_value)).setText(Long.toString(storeTriggerInterval));
    		}
    		break;
    	case R.id.default_config_button:
    		((TextView)findViewById(R.id.init_offset_value)).setText(Integer.toString(DEFAULT_START_OFFSET_SEC));
    		((TextView)findViewById(R.id.update_interval_value)).setText(Integer.toString((int)DEFAULT_UPDATE_INTERVAL_SEC));
    		storeStartOffset = DEFAULT_START_OFFSET_SEC;
    		storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_SEC;
    		storeIntervalIndex = 3;
    		((Spinner)findViewById(R.id.update_interval_select)).setSelection(storeIntervalIndex, true);
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
    
    // Selector
	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int pos,
			long id) {
		storeIntervalIndex = pos;
		Log.d(getClass().getName(), "Select index : " + pos);
		if (pos == 5) {
			((TextView)findViewById(R.id.update_interval_value)).setEnabled(true);
		} else {
			((TextView)findViewById(R.id.update_interval_value)).setEnabled(false);
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

    // Helper methods
    // Restore the state
    private void restoreConfig() {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        // Get configuration
        storeStartOffset = prefs.getInt("AlarmStartOffset", DEFAULT_START_OFFSET_SEC);
        storeTriggerInterval = prefs.getLong("AlarmTriggerInterval", DEFAULT_UPDATE_INTERVAL_SEC);
        alarmSetupDone = prefs.getBoolean("AlarmEnabled", true);
        storeIntervalIndex = prefs.getInt("AlarmIntervalSelect", 3); // Index of half day
    	Log.d(getClass().getName(), "Restored: " + storeStartOffset + ", " + storeTriggerInterval + ", "
    			+ alarmSetupDone + ", " + storeIntervalIndex);

    }

    // Update persistent configuration
    private void updateConfig() {
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	SharedPreferences.Editor prefEdit = prefs.edit();
    	prefEdit.putInt("AlarmStartOffset", storeStartOffset);
    	prefEdit.putLong("AlarmTriggerInterval", storeTriggerInterval);
    	prefEdit.putBoolean("AlarmEnabled", alarmSetupDone);
    	prefEdit.putInt("AlarmIntervalSelect", storeIntervalIndex);
    	prefEdit.apply();
    	Log.d(getClass().getName(), "Applied: " + storeStartOffset + ", " + storeTriggerInterval + ", "
    			+ alarmSetupDone + ", " + storeIntervalIndex);
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
