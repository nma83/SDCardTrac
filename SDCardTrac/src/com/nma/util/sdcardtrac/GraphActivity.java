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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Spinner;

public class GraphActivity extends ActionBarActivity
    implements GraphFragment.OnFragmentInteractionListener {

    int messageIndex;
    private static final int DIALOG_CHANGELOG = 1;
    public static final String TAB_NAME_INT_STORAGE = "Internal";
    public static final String TAB_NAME_EXT_STORAGE = "External";
    public static final String SHOW_HELP_TAG = "showHelp";
    private String interval;
    private ActionBar actionBar;
    private Spinner durationSel;
    private boolean forceSettings = false;
    private boolean helpExit = false;
    private boolean showTips = false;
    private boolean alarmEnabled = false;
    private boolean serviceBound = false;

    @Override
    public void onCreate(Bundle savedInstance) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstance);
        setContentView(R.layout.graph);

        // Setup debug logging
        SettingsActivity.ENABLE_DEBUG = pref.getBoolean(SettingsActivity.ENABLE_DEBUG_KEY, false);

        // ActionBar
        actionBar = getSupportActionBar();
        // Add a custom view with a spinner
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.graph_action_bar);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // TEMP
        Log.d(getClass().getName(), "Creating 1");
        actionBar.addTab(actionBar.newTab().setText(TAB_NAME_EXT_STORAGE)
                .setTabListener(new GraphTabListener(this, TAB_NAME_EXT_STORAGE)));
        /*Log.d(getClass().getName(), "Creating 2");
        actionBar.addTab(actionBar.newTab().setText(TAB_NAME_INT_STORAGE)
        .setTabListener(new GraphTabListener(this, TAB_NAME_INT_STORAGE)));*/

        durationSel = (Spinner) findViewById(R.id.graph_action_bar_spinner);
        durationSel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                interval = parent.getItemAtPosition(position).toString();

                refreshGraph(true, interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Help on first
        showTips = pref.getBoolean(SHOW_HELP_TAG, true);
        if (showTips) {
            showHelp();
        }

        // Start service
        alarmEnabled = pref.getBoolean(SettingsActivity.ALARM_RUNNING_KEY, false);
        boolean reInst = false;
        if (savedInstance != null) {
	    Parcelable spinner;
            reInst = savedInstance.getBoolean(SettingsActivity.ALARM_RUNNING_KEY, false);
	    interval = savedInstance.getString("interval", "Day");
	    spinner = savedInstance.getParcelable("spinner");
	    if (spinner != null)
		durationSel.onRestoreInstanceState(spinner);
	}

        if (alarmEnabled && !reInst) {
            Intent serviceIntent = new Intent(this, FileObserverService.class);
            serviceIntent.setAction(Intent.ACTION_MAIN);
            startService(serviceIntent);
        }
    }

    // Menu creating and handling
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.graph_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	case R.id.graph_refresh:
	    refreshGraph(false, interval);
	    return true;
	case R.id.graph_search:
	    return onSearchRequested();
	case R.id.graph_settings:
	    showSettings();
	    return true;
	case R.id.help:
	    showHelp();
	    return true;
        }

        return false;
    }

    // Refresh all graphs
    private void refreshGraph(boolean changeView, String value) {

        if (!changeView) {
            // Trigger collection
            Intent triggerCollect = new Intent(this, FileObserverService.class);
            triggerCollect.setAction(Intent.ACTION_VIEW);
            startService(triggerCollect);
        }

        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag != null) {
                if (frag instanceof GraphFragment &&
                        frag.getArguments() != null) {
                    //((GraphFragment) frag).setTimeInterval(value, frag.isVisible());
                    if (SettingsActivity.ENABLE_DEBUG)
                        Log.d(getClass().getName(), "Refreshing " + frag);
                    if (!changeView)
                        ((GraphFragment) frag).restartLoader();
                    ((GraphFragment) frag).setTimeInterval(value, frag.isVisible());
                }
            }
        }
    }

    // Called from TabListener to create correct viewport
    public String getTimeInterval() {
        return interval;
    }

    // Goto settings menu
    public void showSettings() {
        Intent show = new Intent(this, SettingsActivity.class);
        startActivity(show);
    }

    @Override
    public void onFragmentInteraction(String reason) {
        if (reason != null) {
            if (reason.equals(getString(R.string.act_goto_settings)) ||
                    reason.equals(getString(R.string.exit_help))) {
                //showSettings();
                forceSettings |= reason.equals(getString(R.string.act_goto_settings));
                helpExit |= reason.equals(getString(R.string.exit_help));
                if (forceSettings && helpExit && showTips)
                    showSettings(); // Show settings only on first start once help is closed
            } else {
                // Update the latest fragment
                GraphFragment frag = (GraphFragment) getSupportFragmentManager().findFragmentByTag(reason);
                Log.d("onFragmentInteraction", "Got ID " + reason + "=" + frag);
                if (frag != null)
                    getSupportFragmentManager().beginTransaction()
                            .detach(frag).attach(frag)
                            .commit();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Store flag
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(SHOW_HELP_TAG, false);
        edit.commit();
        showTips = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putBoolean(SettingsActivity.ALARM_RUNNING_KEY, alarmEnabled);
	out.putString("interval", interval);
	out.putParcelable("spinner", durationSel.onSaveInstanceState());
    }

    // Help popup
    private void showHelp() {
        final HelpFragment help = new HelpFragment();
        help.show(getSupportFragmentManager(), "help");
    }
}
