/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import android.util.Log;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.app.SearchManager;
import android.widget.ExpandableListView;
import android.os.Bundle;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

public class SearchableActivity extends FragmentActivity 
    implements LoaderManager.LoaderCallbacks<List<DatabaseLoader.DatabaseRow>> {
    private String query;
    private List<DatabaseLoader.DatabaseRow> locData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.search);

	// Get the intent, verify the action and get the query
	Intent intent = getIntent();
	if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	    query = intent.getStringExtra(SearchManager.QUERY);
	    if (SettingsActivity.ENABLE_DEBUG)
		Log.d(getClass().getName(), "Starting search: " + query);
	    // Load data
	    getSupportLoaderManager().initLoader(0, null, this);
	}
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        setProgressBarIndeterminateVisibility(true);
        return new DatabaseLoader(this, null, query);
    }

    @Override
    public void onLoadFinished(Loader<List<DatabaseLoader.DatabaseRow>> loader,
                               List<DatabaseLoader.DatabaseRow> data) {
	if (SettingsActivity.ENABLE_DEBUG)
	    Log.d(getClass().getName(), "Done loading with " + data.size() + " items");
        locData = data;
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        locData = null;
    }
}
