/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

public class GraphActivity extends ActionBarActivity
    implements GraphFragment.OnFragmentInteractionListener {

	int messageIndex;
	private static final int DIALOG_CHANGELOG = 1;
    public static final String TAB_NAME_INT_STORAGE = "Internal";
    public static final String TAB_NAME_EXT_STORAGE = "External";
    private String interval;
    ActionBar actionBar;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		Spinner durationSel;

		super.onCreate(savedInstance);
		setContentView(R.layout.graph);

        // ActionBar
        actionBar = getSupportActionBar();
        // Add a custom view with a spinner
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.graph_action_bar);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // TEMP
        actionBar.addTab(actionBar.newTab().setText(TAB_NAME_EXT_STORAGE)
        .setTabListener(new GraphTabListener(this, TAB_NAME_EXT_STORAGE)));
        actionBar.addTab(actionBar.newTab().setText(TAB_NAME_INT_STORAGE)
        .setTabListener(new GraphTabListener(this, TAB_NAME_INT_STORAGE)));

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
	    }
	    
	    return false;
	}

    // Refresh all graphs
    private void refreshGraph(String value) {
        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag != null) {
                ((GraphFragment)frag).setTimeInterval(value, frag.isVisible());
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
}
