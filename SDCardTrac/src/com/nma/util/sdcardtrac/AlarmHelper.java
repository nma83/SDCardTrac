/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Class to encapsulate AlarmManager calls

public class AlarmHelper {
	private Context ctx;
	// Alarms
	PendingIntent alarmIntent;
    
	public AlarmHelper(Context c) {
		ctx = c;
        alarmIntent = null;
	}
	
    // Setup recurring collection of events
    public boolean manageAlarm(boolean enable, boolean alarmSetupWasDone, int startOffset, long triggerInterval) {
    	boolean alarmSetupDone = false;

        if (alarmIntent == null) {
            Intent triggerIntent = new Intent(ctx, DeltaCompute.class);
            alarmIntent = PendingIntent.getBroadcast(ctx, 0, triggerIntent, 0);
        }

    	if (enable) {
    		if (alarmSetupWasDone) {
    			((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
    		}
            // Setup the alarm
            setupAlarm(startOffset, triggerInterval);
			alarmSetupDone = true;
    	} else if (alarmSetupWasDone) {
			((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent);
			Log.d(getClass().getName(), "Cancelling alarms");
			alarmSetupDone = false;
    	}
    	
    	return alarmSetupDone;
    }
    
    // Alarm setup
    private void setupAlarm(int startOffset, long triggerInterval) {
    	Log.d(this.getClass().getName(), "Setting up alarm for collection");

        AlarmManager alarmEr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
    	Calendar time = Calendar.getInstance();
    	time.setTimeInMillis(System.currentTimeMillis());
    	time.add(Calendar.SECOND, startOffset);
    	alarmEr.setInexactRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), 
    			triggerInterval, alarmIntent); //AlarmManager.INTERVAL_FIFTEEN_MINUTES
    	Log.d(this.getClass().getName(), "Done with alarm setup: " + triggerInterval);
    }
}
