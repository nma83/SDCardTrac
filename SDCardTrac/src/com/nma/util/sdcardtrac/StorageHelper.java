package com.nma.util.sdcardtrac;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;

/**
 * StorageHelper enumerates the available types of storage
 */
public class StorageHelper {
    private static String [] storagePaths;
    private static final String LOG_TAG = "StorageHelper";

    public static String[] getStoragePaths(Context ctx) {
        if (storagePaths == null) {
            File [] files;
            int i = 0;

            files = ContextCompat.getExternalFilesDirs(ctx, null);
            storagePaths = new String[files.length];
            for (File f : files) {
                try {
                    storagePaths[i] = f.getCanonicalPath();
                    Log.d(LOG_TAG, "Adding path to storage list: " + storagePaths[i]);
                } catch (Exception e) {
                    storagePaths[i] = null;
                    Log.w(LOG_TAG, "Error fetching name of storage path[" + i + "]:\n "
                            + e.toString());
                }
                i++;
            }
        }

        return storagePaths;
    }
}
