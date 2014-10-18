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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.app.SearchManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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
    //private int numItemsOnScreen;
    //private static final int SCREEN_FRACTION = 80; // 80% of screen for list

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
/*
        // A rough guess at number of items to fit in a screen
        int dispHeight = 0, dispWidth = 0;
        Point size = new Point();
        WindowManager w = getWindowManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            w.getDefaultDisplay().getSize(size);
            dispHeight = size.y;
            dispWidth = size.x;
        } else {
            Display d = w.getDefaultDisplay();
            dispHeight = d.getHeight();
            dispWidth = d.getWidth();
        }

        int textHeight = getTextHeight(this, getString(R.string.search_no_data),
                (int) GraphFragment.GRAPHVIEW_TEXT_SIZE_DIP, dispWidth);
        if (textHeight == 0) textHeight = 1; // Safety
        // 3 lines of text, fit into fraction of the screen
        numItemsOnScreen = (dispHeight * SCREEN_FRACTION) / (100 * textHeight * 3);
        // Ceil
        numItemsOnScreen = ((numItemsOnScreen + 10) / 10) * 10;
        Log.d(getClass().getName(), "Calculated screen height=" + dispHeight + ", text height="
                        + textHeight + ", num items=" + numItemsOnScreen);
                        */
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
            // Replace spaces with wildcard
            //query = query.replace(' ', '%');
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
        showResult(0);
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

    // Show a page of results
    private void showResult(int startPage) {
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
/*
        listStart = startPage * numItemsOnScreen;
        listEnd = listStart + numItemsOnScreen;
        if (listEnd > locData.size())
            listEnd = locData.size();
        Log.d(getClass().getName(), "Fetching from " + listStart + " to " + listEnd);
        pageData = (ArrayList)locData.subList(listStart, listEnd);
*/
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

    public static int getTextHeight(Context context, CharSequence text, int textSize,
                                    int deviceWidth) { //, Typeface typeface,int padding) {
        TextView textView = new TextView(context);
        textView.setPadding(0, 0, 0, 0);
        textView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        textView.setText(text, TextView.BufferType.SPANNABLE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        return textView.getMeasuredHeight();
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
