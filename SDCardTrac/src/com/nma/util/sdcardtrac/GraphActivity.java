/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

public class GraphActivity extends ActionBarActivity
    implements GraphFragment.OnFragmentInteractionListener {

	int messageIndex;
	private static final int DIALOG_CHANGELOG = 1;
    public static final String TAB_NAME_INT_STORAGE = "Internal";
    public static final String TAB_NAME_EXT_STORAGE = "External";
    public static final String SHOW_HELP_TAG = "showHelp";
    private String interval;
    ActionBar actionBar;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		Spinner durationSel;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showTips = pref.getBoolean(SHOW_HELP_TAG, true);

		super.onCreate(savedInstance);
		setContentView(R.layout.graph);

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

        durationSel = (Spinner)findViewById(R.id.graph_action_bar_spinner);
        durationSel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                interval = parent.getItemAtPosition(position).toString();

                refreshGraph(interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Help on first
        if (showTips) {
            showHelp();
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(SHOW_HELP_TAG, false);
            edit.commit();
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
	            refreshGraph(interval);
	            return true;
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
    private void refreshGraph(String value) {
        // Trigger collection
        Intent triggerCollect = new Intent(this, FileObserverService.class);
        triggerCollect.setAction(Intent.ACTION_VIEW);
        startService(triggerCollect);

        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag != null) {
                if (frag instanceof GraphFragment &&
                        frag.getArguments() != null) {
                    ((GraphFragment) frag).setTimeInterval(value, frag.isVisible());
                    Log.d(getClass().getName(), "Refreshing " + frag);
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
        if (reason != null &&
            reason.equals(getString(R.string.act_goto_settings))) {
            showSettings();
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

    // Help popup
    private void showHelp() {
        final HelpFragment help = new HelpFragment();
        help.show(getSupportFragmentManager(), "help");
    }
}
