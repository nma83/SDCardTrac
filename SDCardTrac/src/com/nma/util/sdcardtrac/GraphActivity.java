/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class GraphActivity extends Activity {
	DatabaseManager trackingDB;
	long startTime, endTime;
	long maxStorage;
	boolean lowerThanMax;
	String usageRatio;
	String prevDate;
	String [] logMessages;
	int messageIndex;
	private static final int DIALOG_CHANGELOG = 1;
	private GraphView storageGraph;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		
		super.onCreate(savedInstance);
		setContentView(R.layout.graph);
		
		// Get data from DB
		// Show indefinite progress
		ProgressDialog prog = ProgressDialog.show(this, "Storage history", "Fetching data...", true);
		trackingDB = new DatabaseManager(this);
		GraphViewSeries storageGraphData = getData();
		prog.dismiss();
		
		// Sanity check the data
		if ((storageGraphData == null) || (logMessages.length < 1)) {
			AlertDialog.Builder alertBuild = new AlertDialog.Builder(this);
			alertBuild.setMessage("Database is empty! There seems to be no activity observed.")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					//@Override
					public void onClick(DialogInterface dialog, int which) {
						GraphActivity.this.finish();
					}
				});
			AlertDialog alert = alertBuild.create();
			alert.show();
			return;
		}
		
		// Plot it
		prevDate = "";
		storageGraph = new LineGraphView(this, "Storage History, " + usageRatio) {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {
				String retValue;
				
				if (isValueX) { // Format time in human readable form
					Date currDate = new Date((long)value);
					retValue = DateFormat.getDateInstance().format(currDate);
					if (retValue.equals(prevDate)) {
						prevDate = retValue;
						retValue = DateFormat.getTimeInstance().format(currDate);
					} else {
						retValue = DateFormat.getTimeInstance().format(currDate) + ";" + retValue;
						prevDate = DateFormat.getDateInstance().format(currDate);
					}
//					Log.d(getClass().getName(), "Date is : " + retValue);
				} else { // Format size in human readable form
					retValue = convertToStorageUnits(value);
				}
				//return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
				return retValue;
			}
		};
		// Add selector callback
		storageGraph.setSelectHandler(storageGraph.new OnSelectHandler() {
			@Override
			protected void onSelect(int index) {
				Log.d(getClass().getName(), "In select handler!! @ " + index);
				messageIndex = index;
				showDialog(DIALOG_CHANGELOG);
			}
		});

		storageGraph.setManualYAxis(true);
		storageGraph.setManualYAxisBounds(maxStorage, 0);
		storageGraph.setScalable(true);
		storageGraph.setMultiLineXLabel(true, ";");
		((LineGraphView)storageGraph).setDrawBackground(true);
		storageGraph.addSeries(storageGraphData);
		((LinearLayout)findViewById(R.id.graph_layout)).addView(storageGraph);
	}
	
	// Dialog method
	protected Dialog onCreateDialog(int id) {
		Dialog ret;
		if (id == DIALOG_CHANGELOG) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Change log at " + logMessages[messageIndex])
			       .setCancelable(true);
			ret = builder.create();
		} else {
			ret = null;
		}
		
		return ret;
	}
	
	// Update message everytime
	protected void onPrepareDialog(int id, Dialog dia) {
		if (id == DIALOG_CHANGELOG) {
			((AlertDialog)dia).setMessage("Change log at " + logMessages[messageIndex]);
		}
	}
	
	// Helpers
	private GraphViewSeries getData() {
		int maxUsage = 0;
		
		trackingDB.openToRead();
		// TODO: use start/end time hooks
		ArrayList <ContentValues> dbData = (ArrayList<ContentValues>)trackingDB.getValues(0, 0);
		
		// Build the graph
		GraphViewSeries retSeries;
		Log.d(getClass().getName(), "Creating data of len " + dbData.size());
		GraphViewData [] graphData = new GraphViewData[dbData.size()];
		logMessages = new String[dbData.size()];
		int i = 0;
		for (ContentValues d : dbData) {
			long timeStamp = Long.parseLong((String)d.get(DatabaseManager.ID_COLUMN));
			int usage = Integer.parseInt((String)d.get(DatabaseManager.DELTA_COLUMN));
			String changeLog = (String)d.get(DatabaseManager.LOG_COLUMN);
			logMessages[i] = DateFormat.getDateTimeInstance().format(timeStamp)
					+ ":\n" + changeLog;
			
			if (i == 0) startTime = timeStamp;
			else if (i == (dbData.size() - 1)) endTime = timeStamp;
			
			graphData[i++] = new GraphViewData(timeStamp, usage);
			if (usage > maxUsage) maxUsage = usage;
		}
		
		retSeries = new GraphViewSeries(graphData);
		trackingDB.close();
		maxStorage = Environment.getExternalStorageDirectory().getTotalSpace();

		String usageRatioInt = new DecimalFormat("#.#").format((maxUsage * 100f) / maxStorage);
		usageRatio = convertToStorageUnits(maxUsage) + " max used (" 
				+ usageRatioInt + "%) out of total size " + convertToStorageUnits(maxStorage);
		if ((maxUsage / maxStorage) < 0.7) {
			maxStorage = maxUsage;
			lowerThanMax = true;
		} else {
			lowerThanMax = false;
		}
		
		return retSeries;
	}
	
	private String convertToStorageUnits(double value) {
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
	
	// Refresh the graph with latest data
	private void refreshGraph() {
		// Get data from DB
		// Show indefinite progress
		ProgressDialog prog = ProgressDialog.show(this, "Storage history", "Fetching data...", true);
		trackingDB = new DatabaseManager(this);
		GraphViewSeries storageGraphData = getData();
		prog.dismiss();
		
		// Sanity check the data
		if ((storageGraphData == null) || (logMessages.length < 1)) {
			AlertDialog.Builder alertBuild = new AlertDialog.Builder(this);
			alertBuild.setMessage("Database is empty! There seems to be no activity observed.")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					//@Override
					public void onClick(DialogInterface dialog, int which) {
						GraphActivity.this.finish();
					}
				});
			AlertDialog alert = alertBuild.create();
			alert.show();
			return;
		}
		
		storageGraph.removeSeries(0);
		storageGraph.addSeries(storageGraphData);
		storageGraph.invalidate();
	}
	
	// Menu creating and handling
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.graph_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.graph_refresh:
	            refreshGraph();
	            return true;
	    }
	    
	    return false;
	}
}
