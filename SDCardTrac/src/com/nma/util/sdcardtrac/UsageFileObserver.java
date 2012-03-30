package com.nma.util.sdcardtrac;

import android.os.FileObserver;
import android.util.Log;

public class UsageFileObserver extends FileObserver {
    private FileObserverService notifyThis;

    public UsageFileObserver (String filePath, int eventMask, FileObserverService notifyMe) {
        super(filePath, eventMask);
        notifyThis = notifyMe;
    }

    @Override
    public void onEvent(int event, String path) {
    	Log.d(getClass().getName(), "Event seen: " + event + " @ " + path);
        notifyThis.queueEvent(path, event); // Send to service
    }
}
