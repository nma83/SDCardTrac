package com.nma.util.sdcardtrac;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
    implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final long DEFAULT_UPDATE_INTERVAL_MSEC = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final int DEFAULT_START_OFFSET_SEC = 10;
    public static String ALARM_RUNNING_KEY = "enable_tracker";
    public static String APP_BOOTED_KEY = "app_booted";
    public static String STORE_TRIGGER_KEY = "tracker_update_interval";

    // Preferences
    private long storeTriggerInterval = DEFAULT_UPDATE_INTERVAL_MSEC;
    private boolean setupDone = false;
    // State
    private boolean alarmRunning = false;
    private boolean boundToService = false;
    AlarmHelper alarmHelp;

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
        // Control the service
        Intent serviceIntent = new Intent(this, FileObserverService.class);

        if (enable) {
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
        //Log.d(getClass().getName(), "SharedPref change: " + key + "-" + ALARM_RUNNING_KEY);
        if (key.equals(ALARM_RUNNING_KEY)) {
            //CheckBoxPreference p = (CheckBoxPreference)findPreference(key);
            boolean keyVal;
            keyVal = sharedPreferences.getBoolean(key, false);
            if (keyVal) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    alarmRunning = alarmHelp.manageAlarm(true, alarmRunning, DEFAULT_START_OFFSET_SEC,
                            storeTriggerInterval);
                    backgroundService(true);
                    Log.d(getClass().getName(), "Enabling alarms: " + storeTriggerInterval);
                } else {
                    // TODO make a dialog
                    Toast.makeText(this, R.string.media_not_mounted, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Disable alarms
                alarmRunning = alarmHelp.manageAlarm(false, alarmRunning, DEFAULT_START_OFFSET_SEC,
                        storeTriggerInterval);
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
        }
    }
}
