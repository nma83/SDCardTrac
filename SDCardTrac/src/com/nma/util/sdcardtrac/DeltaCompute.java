/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// Class which wakes up based on alarms and triggers database update
// Also wakes up on boot up

public class DeltaCompute extends BroadcastReceiver {
	// Alarm
	AlarmHelper alarmHelp;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.d(getClass().getName(), "Triggered by <" + intent.getAction() + ">...");
		String intentName;
		
		if (intent.getAction() != null) {
			intentName = intent.getAction();
		} else {
			intentName = "NO_INTENT";
		}
		
		if (intentName.equals(Intent.ACTION_BOOT_COMPLETED)) {
			// Setup the alarms if triggered on boot
			// Get preferences first to determine the alarm parameters
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			// Get configuration
			int startOffset = prefs.getInt("AlarmStartOffset", // UNUSED
                    SettingsActivity.DEFAULT_START_OFFSET_SEC);
            String trigPref = prefs.getString(SettingsActivity.STORE_TRIGGER_KEY, null);
			long triggerInterval;

            if (trigPref != null)
                triggerInterval = Long.parseLong(trigPref);
            else
                triggerInterval = SettingsActivity.DEFAULT_UPDATE_INTERVAL_MSEC;

			boolean alarmEnb = prefs.getBoolean(SettingsActivity.ALARM_RUNNING_KEY, false);

			if (alarmEnb) {
				alarmHelp = new AlarmHelper(ctx);
				alarmHelp.manageAlarm(true, false, startOffset, triggerInterval);

				/*SharedPreferences.Editor prefEdit = prefs.edit();
				prefEdit.putBoolean(, true);
				prefEdit.apply();*/

				Log.d(getClass().getName(), "Enabled alarms after boot!");
			}
		} else {
			// Trigger data collection if normal alarm
			Intent triggerCollect = new Intent(ctx, FileObserverService.class);
			if (intentName.equals(Intent.ACTION_MEDIA_MOUNTED)) {
				triggerCollect.setAction(Intent.ACTION_SYNC);
			} else {
				triggerCollect.setAction(Intent.ACTION_VIEW);
			}
			ctx.startService(triggerCollect); // TODO: Wake lock to be managed by service!
		}
	}
}
