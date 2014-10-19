/*
 *  StorageTrac application - keeps track of the external storage usage
 *  Copyright (C) 2012 Narendra M.A.
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nma.util.sdcardtrac;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.util.Calendar;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final long DEFAULT_UPDATE_INTERVAL_MSEC = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final int DEFAULT_START_OFFSET_SEC = 10;
    public static String ALARM_RUNNING_KEY = "enable_tracker";
    public static String APP_BOOTED_KEY = "app_booted";
    public static String STORE_TRIGGER_KEY = "tracker_update_interval";
    public static String ENABLE_DEBUG_KEY = "enable_debug";
    public static String SHOW_HIDDEN_KEY = "show_hidden";
    public static String BITCOIN_KEY = "pref_bitcoin_key";
    public static String DELETE_DATA_KEY = "tracker_delete_data";
    private static final String BITCOIN_ADDRESS = "16bxTv1fP8X2QN5SWXc1AcKhhA1tJQKcTa";
    private static final int BITCOIN_REQ_ID = 0;
    public static boolean ENABLE_DEBUG = false;

    // Preferences
    private long storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_MSEC;
    private boolean setupDone = false;
    // State
    private boolean alarmRunning = false;
    private boolean boundToService = false;
    private AlarmHelper alarmHelp;
    private CheckBoxPreference alarmChk;

    // Handle to running service
    private FileObserverService.TrackingBinder serviceBind;
/*
    // Show settings
    public static class SettingsPrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInst) {
            super.onCreate(savedInst);
            addPreferencesFromResource(R.xml.preferences);
        }
    }
*/

    // Task to start service
    public class StartWatching extends AsyncTask<Boolean, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Boolean... params) {
            // Control the service
            Intent serviceIntent = new Intent(SettingsActivity.this, FileObserverService.class);

            if (params[0]) {
                serviceIntent.setAction(Intent.ACTION_MAIN);
                startService(serviceIntent);
                // Bind to service
                //bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
            } else {
                //unbindService(serviceConn);
                //boundToService = false;
                serviceIntent.setAction(Intent.ACTION_DELETE);
                startService(serviceIntent);
                // TODO: stopService(serviceIntent);
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (alarmChk != null) {
                alarmChk.setEnabled(true);
                if (result)
                    alarmChk.setSummary(R.string.enabled_tracker);
                else
                    alarmChk.setSummary(R.string.disabled_tracker);
            }
        }
    }

    // Connection to service
    private ServiceConnection serviceConn = new ServiceConnection() {
        //@Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            serviceBind = (FileObserverService.TrackingBinder)service;
            boundToService = true;
        }
        //@Override
        public void onServiceDisconnected(ComponentName className) {
            boundToService = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean started;
        super.onCreate(savedInstanceState);
        // If fresh start, do bootstrap to create the alarm and DB first
        alarmHelp = new AlarmHelper(this);
        started = doBootStrap();
/*        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsPrefFragment())
                .commit();*/
        addPreferencesFromResource(R.xml.preferences);

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        findPreference(BITCOIN_KEY).setOnPreferenceClickListener(this);
        alarmChk = (CheckBoxPreference)findPreference(ALARM_RUNNING_KEY);
        if (alarmRunning)
            alarmChk.setSummary(R.string.enabled_tracker);
        else
            alarmChk.setSummary(R.string.disabled_tracker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    // Check for previously stored settings
    private boolean doBootStrap() {
        SharedPreferences sharedPref;
        SharedPreferences.Editor editor;
        String storeTriggerIntervalS;

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        setupDone = sharedPref.getBoolean(APP_BOOTED_KEY, false);

        // Get default values
        alarmRunning = sharedPref.getBoolean(ALARM_RUNNING_KEY, false);
        storeTriggerIntervalS = sharedPref.getString(STORE_TRIGGER_KEY,
                Long.toString(DEFAULT_UPDATE_INTERVAL_MSEC));
        storeTriggerInterval = Long.parseLong(storeTriggerIntervalS); //, DEFAULT_UPDATE_INTERVAL_SEC);

        if (setupDone) {
            // Everything is running
            return true;
        }

        // Startup things
        if (alarmRunning) {
            // TODO use StorageHelper
            String [] stores = StorageHelper.getStoragePaths(this);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                backgroundService(true);
                alarmRunning = alarmHelp.manageAlarm(true, alarmRunning, DEFAULT_START_OFFSET_SEC,
                        storeTriggerInterval);
            } else {
                Log.e(getClass().getName(), "External media not mounted!");
                // TODO make a dialog
                Toast.makeText(this, R.string.media_not_mounted, Toast.LENGTH_SHORT).show();
            }
        }
        // Set done
        editor = sharedPref.edit();
        editor.putBoolean(APP_BOOTED_KEY, true);
        editor.commit();
        Log.d(getClass().getName(), "Bootstrap done: " + alarmRunning + "," + storeTriggerInterval);
        return false;
    }

    // Start the background service
    public void backgroundService(boolean enable) {
        new StartWatching().execute(enable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (boundToService) {
            unbindService(serviceConn);
            boundToService = false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(getClass().getName(), "SharedPref change: " + key);
        if (key.equals(ALARM_RUNNING_KEY)) {
            //CheckBoxPreference p = (CheckBoxPreference)findPreference(key);
            boolean keyVal;
            keyVal = sharedPreferences.getBoolean(key, false);
            if (keyVal) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    alarmRunning = alarmHelp.manageAlarm(true, alarmRunning, DEFAULT_START_OFFSET_SEC,
                            storeTriggerInterval);
                    alarmChk.setEnabled(false);
                    alarmChk.setSummary(R.string.enabling_tracker);
                    backgroundService(true);
                    if (ENABLE_DEBUG)
                        Log.d(getClass().getName(), "Enabling alarms: " + storeTriggerInterval);
                } else {
                    // TODO make a dialog
                    Toast.makeText(this, R.string.media_not_mounted, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Disable alarms
                alarmRunning = alarmHelp.manageAlarm(false, alarmRunning, DEFAULT_START_OFFSET_SEC,
                        storeTriggerInterval);
                alarmChk.setSummary(R.string.disabled_tracker);
            }
        } else if (key.equals(STORE_TRIGGER_KEY)) {
            if (alarmRunning) {
                ListPreference p = (ListPreference)findPreference(key);
                String selS = p.getValue();
                        //sharedPreferences.getString(key,
                        //Long.toString(DEFAULT_UPDATE_INTERVAL_SEC));
                //Log.d(getClass().getName(), "Got pref=" + p + "=" + selS);
                storeTriggerInterval = Long.parseLong(selS); //, DEFAULT_UPDATE_INTERVAL_SEC);
                // Update alarms
                alarmRunning = alarmHelp.manageAlarm(true, alarmRunning, DEFAULT_START_OFFSET_SEC,
                        storeTriggerInterval);
                Log.d(getClass().getName(), "Changing alarms: " + selS);
            }
        } else if (key.equals(DELETE_DATA_KEY)) {
            deleteData(Integer.parseInt(sharedPreferences.getString(DELETE_DATA_KEY, "0")));
        } else if (key.equals(ENABLE_DEBUG_KEY)) {
            boolean keyVal = sharedPreferences.getBoolean(key, false);
            Log.d(getClass().getName(), "Debug log enable was " + ENABLE_DEBUG + ", is " + keyVal);
            ENABLE_DEBUG = keyVal;
        } else if (key.equals(BITCOIN_KEY)) {
            BitcoinIntegration.requestForResult(this, BITCOIN_REQ_ID, BITCOIN_ADDRESS);
        }
    }

    // For donation
    @Override
    public boolean onPreferenceClick(Preference preference)
    {
        String key = preference.getKey();

        if (key.equals(BITCOIN_KEY))
            BitcoinIntegration.requestForResult(this, BITCOIN_REQ_ID, BITCOIN_ADDRESS);

        return false;
    }

    // Delete data from DB
    private void deleteData(int deleteBefore) {
        DatabaseManager db;
        Calendar calcView;
        long startMillis;

        calcView = Calendar.getInstance();
        calcView.add(Calendar.SECOND, -deleteBefore);
        startMillis = calcView.getTimeInMillis();

        // Fork this off
        db = new DatabaseManager(this);
        db.openToWrite();
        if (deleteBefore == 0)
            db.deleteAll();
        else
            db.deleteRows(startMillis);
        Toast.makeText(this, R.string.data_deleted, Toast.LENGTH_SHORT).show();
    }

    // Bitcoin donation callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BITCOIN_REQ_ID && resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.donate_thanks, Toast.LENGTH_SHORT).show();
        }
    }
}
