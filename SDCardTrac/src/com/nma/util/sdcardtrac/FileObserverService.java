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
    private static final int FOBS_EVENTS_TO_LISTEN = (FileObserver.CLOSE_WRITE | FileObserver.CREATE | FileObserver.DELETE | 
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
     
        for (File i : fromDir.listFiles()) {
            try {
            	if (i.isDirectory()) {
            		// Recursive call
            		locError = parseDirAndHook(i, !hookObs);
            		if (hookObs && !locError) { // Place observer on even levels
            			// Observer will call queueEvent(filePath, eventMask) of this service
            			UsageFileObserver currObs = new UsageFileObserver(i.getPath(), FOBS_EVENTS_TO_LISTEN, this);
            			fobsList.add(currObs); // ID of this observer is the list index
            			numObs++;
            			Log.d(this.getClass().getName(), "Hooking to " + i.getPath());
            			currObs.startWatching();
            		}
            	}
            } catch (NullPointerException e) {
            	locError = true;
            	Log.e(this.getClass().getName(), "Error hooking to " + i.getPath() + "=" + e.toString());
            }
        }
        

        return locError;
    }

    public void queueEvent(String filePath, int eventMask) {
        ObservedEvent currEvent = new ObservedEvent();
        currEvent.filePath = filePath; currEvent.eventMask = eventMask;
        eventsList.add(currEvent);
        // Look at any FileObserver manipulations needed due to file updates
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.d(this.getClass().getName(), "Starting the service");
        // First call to above parser from interface
    	numObs = 0;
    	fobsList = new ArrayList <UsageFileObserver> ();
    	eventsList = new ArrayList <ObservedEvent> ();
        parseDirAndHook(Environment.getExternalStorageDirectory(), true);

        Log.d(this.getClass().getName(), "Done hooking all observers (" + numObs + " of them)");
        return START_STICKY; // Continue running after return
    }

    // Return handle to service so that tracker can collect events
    @Override
    public IBinder onBind(Intent intent) {
        return locBinder;
    }

    // Consumer of eventsList
    public ObservedEvent[] getAllEvents () {
        ObservedEvent[] retArr = (ObservedEvent[]) eventsList.toArray();
        eventsList.clear();
        return retArr;
    }
}
