/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.app.SearchManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.os.Bundle;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SearchableActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<List<DatabaseLoader.DatabaseRow>> {
    private String query;
    private List<DatabaseLoader.DatabaseRow> locData;
    private MyExpandableListAdapter adapter;
    private ProgressBar progBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        // Get the intent, verify the action and get the query
        //if (savedInstanceState == null)
        handleIntent(getIntent());

        adapter = new MyExpandableListAdapter(this, false);
        progBar = (ProgressBar)findViewById(R.id.search_progress);

        /*if (locData != null)
            showResult();*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            if (SettingsActivity.ENABLE_DEBUG)
                Log.d(getClass().getName(), "Starting search: " + query);
            // Load data
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        setProgressBarIndeterminateVisibility(true);
        if (progBar != null) {
            progBar.setProgress(0);
            progBar.setVisibility(View.VISIBLE);
        }

        return new DatabaseLoader(this, null, query);
    }

    @Override
    public void onLoadFinished(Loader<List<DatabaseLoader.DatabaseRow>> loader,
                               List<DatabaseLoader.DatabaseRow> data) {
        if (SettingsActivity.ENABLE_DEBUG)
            Log.d(getClass().getName(), "Done loading with " + data.size() + " items");
        locData = data;
        showResult();
    }

    @Override
    public void onLoaderReset(Loader loader) {
        locData = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchViewItem = menu.findItem(R.id.search_again);
        SearchView searchView = (SearchView) searchViewItem.getActionView();
        if (searchView != null)
            searchView.setIconifiedByDefault(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search_again:
                return onSearchRequested();
            case R.id.search_settings:
                showSettings();
                return true;
        }

        return false;
    }

    // Goto settings menu
    public void showSettings() {
        Intent show = new Intent(this, SettingsActivity.class);
        startActivity(show);
    }

    private void showResult() {
        ExpandableListView list;
        TextView text;
        int groupPos = 0;
        boolean showHidden = false;
        int hidden = 0, total = 0;

        HashMap <String, Integer> fileMap = new HashMap<String, Integer>();

        setProgressBarIndeterminateVisibility(false);

        // Fill in the list
        list = (ExpandableListView)findViewById(R.id.search_list);
        text = (TextView)findViewById(android.R.id.empty);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        showHidden = prefs.getBoolean(SettingsActivity.SHOW_HIDDEN_KEY, false);

        adapter.clear();

        for (DatabaseLoader.DatabaseRow i : locData) {
            long timeStamp;
            String changeLog[];

            timeStamp = i.getTime();
            changeLog = i.getChangeLog().split("\n");

            // Parse file names
            for (String file : changeLog) {
                if (file.matches(".*" + query + ".*")) {
                    String name[];
                    name = file.split(":");
                    if (name.length < 2)
                        continue;

                    String str = convertToString(name[0], timeStamp);
                    total++;
                    if (!showHidden && name[1].matches(".*/\\..*")) {
                        hidden++;
                        continue;
                    }

                    if (fileMap.containsKey(name[1])) {
                        groupPos = fileMap.get(name[1]);
                        adapter.addChild(groupPos, str);
                    } else {
                        adapter.addGroup(name[1]);
                        fileMap.put(name[1], groupPos);
                        adapter.addChild(groupPos, str);
                        groupPos++;
                    }
                }
            }
        }

        list.setAdapter(adapter);

        String done, plural = "";
        if (total != 1) plural = "s";

        done = Integer.toString(total) + " result" + plural;
        if (!showHidden && hidden > 0)
            done = done + ", " + hidden + " hidden";
        text.setText(done);
        progBar.setProgress(100);
        progBar.setVisibility(View.GONE);
    }

    private static String convertToString(String name, long time) {
        // Convert status char codes to readable text
        String ret;
        char stat = name.charAt(0);
        switch (stat) {
            case 'C': ret = "Created"; break;
            case 'D': ret = "Deleted"; break;
            case 'M': ret = "Modified"; break;
            default: ret = "Modified";
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd LLL yyyy, KK:mm a");
        Date dateStamp = new Date(time);
        ret = ret + " at " + dateFmt.format(dateStamp);

        return ret;
    }
}
