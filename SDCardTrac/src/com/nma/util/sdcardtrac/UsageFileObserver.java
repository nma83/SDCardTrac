/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.os.FileObserver;
import android.util.Log;

public class UsageFileObserver extends FileObserver {
    private FileObserverService notifyThis;
    String basePath;

    public UsageFileObserver (String filePath, int eventMask, FileObserverService notifyMe) {
        super(filePath, eventMask);
        notifyThis = notifyMe;
        basePath = filePath;
    }

    @Override
    public void onEvent(int event, String path) {
    	String locPath = basePath + "/" + path;
    	//Log.d(getClass().getName(), "Event seen: 0x" + Integer.toHexString(event) + " @ " + locPath);
    	if (path != null) { // Enqueue only if valid, sometimes null returned after delete
    		notifyThis.queueEvent(locPath, event, this); // Send to service
    	}
    }
}
