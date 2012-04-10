/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Class which wakes up based on alarms and triggers database update

public class DeltaCompute extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
//		Toast.makeText(ctx, "I am alarmed!!! Firing collect...", Toast.LENGTH_SHORT).show();
		Log.d(getClass().getName(), "Triggered by alarm, collecting...");
		Intent triggerCollect = new Intent(ctx, FileObserverService.class);
		triggerCollect.setAction(Intent.ACTION_VIEW);
		ctx.startService(triggerCollect); // TODO: Wake lock to be managed by service!
	}
}
