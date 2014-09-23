/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

        public String toString() {
            return filePath + ":" + eventMask;
        }
    }

    private ConcurrentLinkedQueue <ObservedEvent> eventsList; // Periodically re-created list of events

    // Binder to talk to periodic tracking activity
    public class TrackingBinder extends Binder {
	// Fetch events
        public ObservedEvent[] getAllEvents () {
            ObservedEvent[] retEvents = (ObservedEvent[]) eventsList.toArray();
            eventsList.clear();
            return retEvents;
        }
	// Service handle
	public FileObserverService getService() {
	    return FileObserverService.this;
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
    	//Log.d(this.getClass().getName(), "Got intent : " + intent.getAction());
    	if ((intent == null) || (intent.getAction().equals(Intent.ACTION_MAIN))) {
    	    // Restart by OS or activity
    	    if (trackingDB != null) {
                trackingDB.openToRead();
    	        List <ContentValues> tmpList = trackingDB.getValues(basePath, 0, 0);
                trackingDB.close();
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
        		UsageFileObserver currObs = new UsageFileObserver(fromDir.getPath(),
                        FOBS_EVENTS_TO_LISTEN, this);
        		fobsList.add(currObs); // ID of this observer is the list index
        		numObs++;
//        		Log.d(this.getClass().getName(), "Hooking to " + fromDir.getPath());
        	}
        } catch (NullPointerException e) {
        	locError = true;
        	Log.e(this.getClass().getName(), "Error hooking to " + fromDir.getPath() +
                    "=" + e.toString());
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
        if (SettingsActivity.ENABLE_DEBUG)
            Log.d(getClass().getName(), "queueEvent " + currEvent.toString());
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
        ObservedEvent [] tmpEv = eventsList.toArray(new ObservedEvent[eventsList.size()]);
    	// Use synchronized version since queueEvent may be modifying the eventsList
    	List <ObservedEvent> uniqEvents = Arrays.asList(tmpEv);
    	//new ArrayList <ObservedEvent> ();
    	
    	if (!ignoreEvents) {
            // Return if no events
            if (eventsList.isEmpty()) {
                Log.w(this.getClass().getName(), "No events yet!");
                return;
            }

            eventsList.clear();
    	}
    	
	// Check if storage is available
	String storeState = Environment.getExternalStorageState();
	if (!(storeState.equals(Environment.MEDIA_MOUNTED) ||
	      storeState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)))
	    return;

    	// Get the space data
    	long totalSpace = Environment.getExternalStorageDirectory().getTotalSpace();
    	long freeSpace = Environment.getExternalStorageDirectory().getFreeSpace();
    	long usedSpace = (totalSpace - freeSpace);
	if (SettingsActivity.ENABLE_DEBUG)
	    Log.d(getClass().getName(), "Total = " + totalSpace + ", free = " + freeSpace);

        // Store in database
        trackingDB.openToWrite();
        HashMap <String, Character> fileEvents = new HashMap<String, Character>();

        // Get most relevant event for a file
        for (ObservedEvent i : uniqEvents) {
            String filePath = i.filePath;
            boolean created = ((i.eventMask & FileObserver.CREATE) != 0);
            boolean deleted = ((i.eventMask & FileObserver.DELETE) != 0);
            boolean moved = ((i.eventMask & FileObserver.MOVED_TO) != 0);
            boolean modified = ((i.eventMask & FileObserver.MODIFY) != 0);

            if (fileEvents.containsKey(filePath)) {
                char newVal = 0;
                // Keep only the latest event flag
                switch ((char)fileEvents.get(filePath)) {
                    case 'C': // Was created
                        // Delete and move override
                        if (moved)
                            newVal = 'V';
                        if (deleted)
                            newVal = 'D';
                        break;
                    case 'D': // Was deleted
                        // Create overrides
                        if (created)
                            newVal = 'C';
                        break;
                    case 'V': // Was moved
                        // Delete and create overrides
                        if (deleted)
                            newVal = 'D';
                        if (created)
                            newVal = 'C';
                        break;
                    case 'M': // Modified
                        // All override
                        newVal = mapEventToChar(i.eventMask);
                        break;
                    default:
                        break;
                }

                if (newVal != 0)
                    fileEvents.put(filePath, newVal);
            } else {
                fileEvents.put(filePath, mapEventToChar(i.eventMask));
            }
        }
        
        // Create message from map of file and events
        numLogs = fileEvents.size();
        if (numLogs == 0)
            numLogs = 1;
        changeLog = new String[numLogs];
        numLogs = 0;
        if (ignoreEvents) {
        	changeLog[0] = "- External event -";
        } else {
        	for (String k : fileEvents.keySet()) {
                if (SettingsActivity.ENABLE_DEBUG)
                    Log.d(getClass().getName(), "fileEvent: " + k);
        		changeLog[numLogs++] = fileEvents.get(k) + ":" + k;
        	}
        	// Sort (not really needed)
        	//Arrays.sort(changeLog);
        }
        
        StringBuilder sb = new StringBuilder();
        for (String s : changeLog)
        {
            sb.append(s);
            sb.append("\n");
        }
        
//        Log.d(this.getClass().getName(), "Inserting row - " + sb.toString());
        trackingDB.insert(GraphActivity.TAB_NAME_EXT_STORAGE, System.currentTimeMillis(),
                usedSpace, sb.toString());
        trackingDB.close();
    }
    
    // Helper
    private char mapEventToChar(int eventMask) {
        char changeTag = 0;
        if ((eventMask & FileObserver.CREATE) != 0)
            changeTag = 'C';
        if ((eventMask & FileObserver.DELETE) != 0)
            changeTag = 'D';
        if ((eventMask & FileObserver.MOVED_TO) != 0)
            changeTag = 'V';
        if (changeTag == 0) {
            if (eventMask != 0)
                changeTag = 'M';
            else
                changeTag = 'N';
        }
        return changeTag;
    }
}
