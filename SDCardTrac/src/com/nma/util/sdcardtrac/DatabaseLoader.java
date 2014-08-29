package com.nma.util.sdcardtrac;

import android.content.ContentValues;
import android.os.Environment;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by naren on 7/10/14.
 */

public class DatabaseLoader extends AsyncTaskLoader <List<DatabaseLoader.DatabaseRow>> {
    private String storageType;

    // Class to store data required for a point in the Graph
    public static class DatabaseRow {
        private final long timeStamp;
        private final long usage;
        private final String changeLog;

        public DatabaseRow(long ts, long us, String cl) {
            timeStamp = ts;
            usage = us;
            changeLog = cl;
        }

        public long getTime() {
            return timeStamp;
        }

        public long getUsage() {
            return usage;
        }

        public String getChangeLog() {
            return changeLog;
        }
    }

    // Database handle
    private final DatabaseManager trackingDB;
    // Result
    private ArrayList<DatabaseRow> loadedDB;

    public DatabaseLoader(Context context, String type) {
        super(context);
        trackingDB = new DatabaseManager(getContext());
        storageType = type;
        //Log.d("SDDBLoader", "Created loader");
    }

    @Override
    public List<DatabaseLoader.DatabaseRow> loadInBackground() {
        long maxUsage = 0;
        ArrayList <DatabaseRow> retVal;
        long maxStorage;

        trackingDB.openToRead();
        // TODO: use start/end time hooks
        ArrayList<ContentValues> dbData = (ArrayList<ContentValues>)trackingDB.getValues(
                storageType, 0, 0);
        retVal = new ArrayList<DatabaseRow>();

        int i = 0;
        for (ContentValues d : dbData) {
            long timeStamp = Long.parseLong((String)d.get(DatabaseManager.ID_COLUMN));
            long usage = Long.parseLong((String)d.get(DatabaseManager.DELTA_COLUMN));
            String changeLog = (String)d.get(DatabaseManager.LOG_COLUMN);
            Date dateStamp = new Date(timeStamp);
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd LLL yyyy, K:m a");
            //changeLog = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            changeLog = dateFmt.format(dateStamp)
                    + ":\n" + changeLog;

            if (usage > maxUsage) maxUsage = usage;
            retVal.add(new DatabaseRow(timeStamp, usage, changeLog));
        }

        trackingDB.close();
        loadedDB = retVal;
        Log.d("SDDBLoader", "Returning size " + retVal.size());

        return retVal;
    }

    // Send result back
    @Override
    public void deliverResult(List<DatabaseRow> ret) {
        //Log.d("SDDBLoader", "deliverResult");
        // Simple return, TODO more checking
        super.deliverResult(loadedDB);
    }

    @Override
    protected void onStartLoading() {
        if (loadedDB != null) {
            // If we currently have a result available, deliver it
            // immediately.
            //Log.d("SDDBLoader", "onStartLoading deliverResult");
            deliverResult(loadedDB);
        }

        if (loadedDB == null) {
            //Log.d("SDDBLoader", "onStartLoading forceLoad");
            forceLoad();
        }
    }

    // Format string to be more readable
    public static String convertToStorageUnits(double value) {
        String retValue;
        long scaling;
        String suffix;

        if ((value / 1000) < 1) { // Under a KB, keep as is
            scaling = 1;
            suffix = "B";
        } else if ((value / 1000000) < 1) { // KB
            scaling = 1000;
            suffix = "KB";
        } else if ((value / 1000000000) < 1) { // MB
            scaling = 1000000;
            suffix = "MB";
        } else { // GB
            scaling = 1000000000;
            suffix = "GB";
        }

        retValue = new DecimalFormat("#.#").format((value / scaling)) + suffix;
        return retValue;
    }
}
