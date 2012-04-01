package com.nma.util.sdcardtrac;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class SDCardTracActivity extends Activity {
	private static final int START_ALARM_OFFSET = 10;
	
	// Handle to running service
	private FileObserverService.TrackingBinder serviceBind;
	boolean boundToService = false;
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
        // Setup the alarm
        setupEventCollection();
        // Setup the database
        trackingDB = new DatabaseManager(this);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	// Start the service
    	Intent serviceIntent = new Intent(this, FileObserverService.class);
    	serviceIntent.setAction(Intent.ACTION_MAIN);
    	startService(serviceIntent);
    	// Bind to service
    	bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    	// Open the database to read
    	trackingDB.openToRead();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if (boundToService) {
    		unbindService(serviceConn);
    		boundToService = false;
    	}
    }
    
    // Helper methods
    // Setup recurring collection of events
    public void setupEventCollection() {
    	Log.d(this.getClass().getName(), "Setting up alarm for collection");
    	AlarmManager alarmEr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
    	Intent triggerIntent = new Intent(this, DeltaCompute.class);
    	PendingIntent pendIntent = PendingIntent.getBroadcast(this, 0, triggerIntent, 0);
    	Calendar time = Calendar.getInstance();
    	time.setTimeInMillis(System.currentTimeMillis());
    	time.add(Calendar.SECOND, START_ALARM_OFFSET);
    	alarmEr.setInexactRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), 
    			(1000 * 30), pendIntent); //AlarmManager.INTERVAL_FIFTEEN_MINUTES
    	Log.d(this.getClass().getName(), "Done with alarm setup");
    }
}
