package com.nma.util.sdcardtrac;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

public class FileObserverService extends Service {

    private List <UsageFileObserver> fobsList; // List of FileObserver objects, must always be referenced by Service in order for the hooks to work
    private static final int FOBS_EVENTS_TO_LISTEN = 
    		(FileObserver.MODIFY | FileObserver.CREATE | FileObserver.DELETE | 
    		 FileObserver.MOVED_FROM | FileObserver.MOVED_TO); // Event mask

    private int numObs;
    
    // Data-structure of a file event
    public class ObservedEvent {
        public String filePath = "";
        public int eventMask = 0;
    }

    private List <ObservedEvent> eventsList; // Periodically re-created list of events

    // Binder to talk to periodic tracking activity
    public class TrackingBinder extends Binder {
        public ObservedEvent[] getAllEvents () {
            ObservedEvent[] retEvents = (ObservedEvent[]) eventsList.toArray();
            eventsList.clear();
            return retEvents;
        }
    }
    private final IBinder locBinder = new TrackingBinder();

    boolean parseDirAndHook (File fromDir, boolean hookObs)
    {
        boolean locError = false;
        
        try {
        	if (hookObs) { // Place observer on even levels, this *doesnt* work, so place on all
        		// The even level strategy would work if FileObserver operation was as it is documented
        		// Observer will call queueEvent(filePath, eventMask) of this service
        		UsageFileObserver currObs = new UsageFileObserver(fromDir.getPath(), FOBS_EVENTS_TO_LISTEN, this);
        		fobsList.add(currObs); // ID of this observer is the list index
        		numObs++;
        		Log.d(this.getClass().getName(), "Hooking to " + fromDir.getPath());
        	}
        } catch (NullPointerException e) {
        	locError = true;
        	Log.e(this.getClass().getName(), "Error hooking to " + fromDir.getPath() + "=" + e.toString());
        }
        
        if (!locError && (fromDir != null) && (fromDir.listFiles() != null)) {
        	for (File i : fromDir.listFiles()) {
        		if (i.isDirectory()) {
        			// Recursive call
        			//Log.d(this.getClass().getName(), "Getting into " + i.getPath());
        			locError = parseDirAndHook(i, hookObs);
        		}
        	}
        }
        
        return locError;
    }

    public void queueEvent(String filePath, int eventMask, UsageFileObserver fileObs) {
        ObservedEvent currEvent = new ObservedEvent();
        currEvent.filePath = filePath; currEvent.eventMask = eventMask;
        eventsList.add(currEvent);
        // Look at any FileObserver manipulations needed due to file/directory updates
        // Hook up observer if a new directory is created
        if ((eventMask & FileObserver.CREATE) != 0 || 
        		(eventMask & FileObserver.MOVED_TO) != 0) {
        	File chkDir = new File(filePath);
        	if (chkDir.isDirectory()) {
        		parseDirAndHook(chkDir, true);
        	}
        	fobsList.get(fobsList.size() - 1).startWatching();
        }
    }

    @Override
    public void onCreate() {
    	Log.d(this.getClass().getName(), "Creating the service");
        // First call to above parser from interface
    	numObs = 0;
    	fobsList = new ArrayList <UsageFileObserver> ();
    	eventsList = new ArrayList <ObservedEvent> ();
        parseDirAndHook(Environment.getExternalStorageDirectory(), true);

        Log.d(this.getClass().getName(), "Done hooking all observers (" + numObs + " of them)");
    }
    
    // Below is called on 2 occasions
    // 1. From activity starting up the whole thing
    // 2. From an alarm indicating to collect events
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// Decode the intent
    	Log.d(this.getClass().getName(), "Got intent : " + intent.getAction());
    	if ((intent == null) || (intent.getAction().equals(Intent.ACTION_MAIN))) { // Restart by OS or activity
    		for (UsageFileObserver i : fobsList) {
    			i.startWatching();
    		}
    		Log.d(this.getClass().getName(), "Started watching...");
    	} else if (intent != null) { // Other actions
    		if (intent.getAction().equals(Intent.ACTION_VIEW)) { // Collect data
    			Log.d(this.getClass().getName(), "Clearing " + eventsList.size() + " events");
    			getAllEvents();
    		}
    	}
    	
        return START_STICKY; // Continue running after return
    }

    @Override
    public void onDestroy() {
    	for (UsageFileObserver i : fobsList) {
    		i.stopWatching();
    	}
    	Log.d(this.getClass().getName(), "Stopped watching...");
    }
    
    // Return handle to service so that tracker can collect events
    @Override
    public IBinder onBind(Intent intent) {
        return locBinder;
    }

    // Consumer of eventsList
    public ObservedEvent[] getAllEvents () {
    	ObservedEvent[] retArr = new ObservedEvent[eventsList.size()];
    	eventsList.toArray(retArr);
        eventsList.clear();
        return retArr;
    }
}
