/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.ContentValues;
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
    private DatabaseManager trackingDB;
    private String basePath;
    
    // Data-structure of a file event
    public class ObservedEvent {
        public String filePath = "";
        public int eventMask = 0;
        public boolean duplicate = false;
        
        public boolean compareWith(ObservedEvent i) {
        	return (filePath.equals(i.filePath) && (eventMask == i.eventMask));
        }
    }

    private ConcurrentLinkedQueue <ObservedEvent> eventsList; // Periodically re-created list of events

    // Binder to talk to periodic tracking activity
    public class TrackingBinder extends Binder {
        public ObservedEvent[] getAllEvents () {
            ObservedEvent[] retEvents = (ObservedEvent[]) eventsList.toArray();
            eventsList.clear();
            return retEvents;
        }
    }
    private final IBinder locBinder = new TrackingBinder();

    @Override
    public void onCreate() {
        File baseDir = Environment.getExternalStorageDirectory();
        basePath = baseDir.getAbsolutePath();
    	Log.d(this.getClass().getName(), "Creating the service");
        // First call to above parser from interface
    	numObs = 0;
    	fobsList = new ArrayList <UsageFileObserver> ();
    	eventsList = new ConcurrentLinkedQueue<FileObserverService.ObservedEvent>();
        parseDirAndHook(baseDir, true);

        // Create database handle
        trackingDB = new DatabaseManager(this);
        Log.d(this.getClass().getName(), "Done hooking all observers (" + numObs + " of them)"
        + " starting at " + basePath);
    }
    
    // Below is called on: 
    // 1. From activity starting up the whole thing
    // 2. From an alarm indicating to collect events
    // 3. Media remounted, to record delta size
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// Decode the intent
//    	Log.d(this.getClass().getName(), "Got intent : " + intent.getAction());
    	if ((intent == null) || (intent.getAction().equals(Intent.ACTION_MAIN))) {
    	    // Restart by OS or activity
    	    if (trackingDB != null) {
                trackingDB.openToRead();
    	        List <ContentValues> tmpList = trackingDB.getValues(basePath, 0, 0);
    	        if (tmpList.isEmpty()) {
        	        // Create a dummy entry
        	        queueEvent(basePath, FileObserver.ATTRIB, null);
    	            storeAllEvents(true);
    	        }
    	    }
    	    
    		for (UsageFileObserver i : fobsList) {
    			i.startWatching();
    		}
    		Log.d(this.getClass().getName(), "Started watching...");
    	} else if (intent != null) { // Other actions
    		if (intent.getAction().equals(Intent.ACTION_VIEW)) { // Collect data
    			Log.d(this.getClass().getName(), "Clearing " + eventsList.size() + " events");
    			storeAllEvents(false);
    		} else if (intent.getAction().equals(Intent.ACTION_SYNC)) {
    			storeAllEvents(true);
    		} else if (intent.getAction().equals(Intent.ACTION_DELETE)) {
    			stopWatching();
    		}
    	}
    	
        return START_STICKY; // Continue running after return
    }

    @Override
    public void onDestroy() {
    	stopWatching();
    }
    
    // Return handle to service so that tracker can collect events
    @Override
    public IBinder onBind(Intent intent) {
        return locBinder;
    }
    
    // Recursively hookup the FileObserver's
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
//        		Log.d(this.getClass().getName(), "Hooking to " + fromDir.getPath());
        	}
        } catch (NullPointerException e) {
        	locError = true;
        	Log.e(this.getClass().getName(), "Error hooking to " + fromDir.getPath() + "=" + e.toString());
        }
        //Log.d(this.getClass().getName(), "Descending into " + fromDir.getPath() + " files " + fromDir.listFiles());
        if (!locError && (fromDir != null) && (fromDir.listFiles() != null)) {
        	for (File i : fromDir.listFiles()) {
        		if (i.isDirectory()) {
        			// Recursive call
        			locError = parseDirAndHook(i, hookObs);
        		}
        	}
        }
        
        return locError;
    }

    // Stop watching
    private void stopWatching() {
    	for (UsageFileObserver i : fobsList) {
    		i.stopWatching();
    	}
    	Log.d(this.getClass().getName(), "Stopped watching...");
    }
    
    // Callback for event recording
    public void queueEvent(String filePath, int eventMask, UsageFileObserver fileObs) {
        ObservedEvent currEvent = new ObservedEvent();
        
        currEvent.filePath = filePath; currEvent.eventMask = eventMask;
        currEvent.duplicate = false; // Will be updated while reporting
        
//        synchronized (syncEventsList) {
        eventsList.add(currEvent);	
//		}
        
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

    // Consumer of eventsList
    public void storeAllEvents (boolean ignoreEvents) {
    	String [] changeLog;
    	int numLogs = 0;
    	// Use synchronized version since queueEvent may be modifying the eventsList
    	ArrayList <ObservedEvent> uniqEvents = new ArrayList <ObservedEvent> ();
    	
    	if (ignoreEvents) {
    		numLogs = 1;
    	} else {
    		// Return if no events
    		if (eventsList.isEmpty()) {
    			Log.w(this.getClass().getName(), "No events yet!");
    			return;
    		}

    		// Compact the change log, remove duplicate entries
    		// TODO: O(n^2) operation here, try to optimise, profile too
    		for (ObservedEvent i : eventsList) {
    			if (!i.duplicate) {
    				for (ObservedEvent j : eventsList) {
    					//    				Log.d(getClass().getName(), "Comparing hashes >" + (i == j) + "<");
    					if (i.compareWith(j) && (i != j))
    						j.duplicate = true;
    				}

    				uniqEvents.add(i);
    				numLogs++;
    			}
    			//    		Log.d(getClass().getName(), "Listing event for: " + i.filePath + "@" + i.duplicate + "@" + numLogs);
    		}

    		eventsList.clear();
    	}
    	
    	// Get the space data
    	long totalSpace = Environment.getExternalStorageDirectory().getTotalSpace();
    	long freeSpace = Environment.getExternalStorageDirectory().getFreeSpace();
    	long usedSpace = (totalSpace - freeSpace);

        // Store in database
        trackingDB.openToWrite();
        changeLog = new String[numLogs];
        numLogs = 0;
        if (ignoreEvents) {
        	changeLog[0] = "- External event -";
        } else {
        	for (ObservedEvent i : uniqEvents) {
        		String changeTag = null;
        		
        		if ((i.eventMask & FileObserver.CREATE) != 0)
        			changeTag = "Created";
        		if ((i.eventMask & FileObserver.DELETE) != 0)
        			changeTag = "Deleted";
        		if ((i.eventMask & FileObserver.MOVED_TO) != 0)
        			changeTag = "Moved";
        		if (changeTag == null) {
        		    if (i.eventMask != 0)
        		//(i.eventMask & FileObserver.MODIFY) != 0)
        			    changeTag = "Modified";
        			else
        			    changeTag = "No event";
        		}

        	    changeTag += ": " + i.filePath;
        		
        		changeLog[numLogs++] = changeTag;
        	}
        }
        
        StringBuilder sb = new StringBuilder();
        for (String s : changeLog)
        {
            sb.append(s);
            sb.append("\n");
        }
        
//        Log.d(this.getClass().getName(), "Inserting row - " + sb.toString());
        trackingDB.insert(GraphActivity.TAB_NAME_EXT_STORAGE, System.currentTimeMillis(),
                (int)usedSpace, sb.toString());
        trackingDB.close();
    }
}
