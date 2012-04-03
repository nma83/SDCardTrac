/*
 * SDCardTrac application - keeps track of the /sdcard usage
 * Copyright (C) 2012 Narendra M.A.
*/

package com.nma.util.sdcardtrac;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;

public class GraphActivity extends Activity {
	DatabaseManager trackingDB;
	long startTime, endTime;
	long maxStorage;
	boolean lowerThanMax;
	String prevDate;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		int locColor;
		
		super.onCreate(savedInstance);
		setContentView(R.layout.graph);
		
		// Get data from DB
		trackingDB = new DatabaseManager(this);
		GraphViewSeries storageGraphData = getData();
		// Plot it
		prevDate = "";
		GraphView storageGraph = new LineGraphView(this, "Storage History") {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {
				String retValue;
				
				if (isValueX) { // Format time in human readable form
					retValue = DateFormat.getDateInstance().format(new Date((long)value));
					if (retValue.equals(prevDate)) {
						prevDate = retValue;
						retValue = DateFormat.getTimeInstance().format(new Date((long)value));
					} else {
						prevDate = retValue;
					}
//					Log.d(getClass().getName(), "Date is : " + retValue);
				} else { // Format size in human readable form
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
					retValue = Integer.toString((int)(value / scaling)) + suffix;
				}
				//return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
				return retValue;
			}
		};

		if (lowerThanMax) {
			locColor = Color.rgb(100, 100, 0); // Current usage is lower than 70%
		} else {
			locColor = Color.rgb(200, 0, 0);
		}
		// Add marker for maximum storage
		GraphViewSeries maxStorageMark = new GraphViewSeries("Maximum storage", locColor,
				new GraphViewData[] {
					new GraphViewData(startTime, maxStorage),
					new GraphViewData(endTime, maxStorage)
		});
		
		// Add marker for 0
		GraphViewSeries minStorageMark = new GraphViewSeries("Maximum storage", Color.rgb(0, 0, 0),
				new GraphViewData[] {
					new GraphViewData(startTime, 0),
					new GraphViewData(endTime, 0)
		});

		
		storageGraph.addSeries(storageGraphData);
		storageGraph.addSeries(maxStorageMark);
		storageGraph.addSeries(minStorageMark);
		((LinearLayout)findViewById(R.id.graph_layout)).addView(storageGraph);
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
		int i = 0;
		for (ContentValues d : dbData) {
			long timeStamp = Long.parseLong((String)d.get(DatabaseManager.ID_COLUMN));
			int usage = Integer.parseInt((String)d.get(DatabaseManager.DELTA_COLUMN));
			String changeLog = (String)d.get(DatabaseManager.LOG_COLUMN); // TODO:
			if (i == 0) startTime = timeStamp;
			else if (i == (dbData.size() - 1)) endTime = timeStamp;
			
			graphData[i++] = new GraphViewData(timeStamp, usage);
			if (usage > maxUsage) maxUsage = usage;
			
//			Log.d(getClass().getName(), "Added to graph : " + Long.parseLong(timeStamp)
//					+ " - " + Integer.parseInt(usage));
		}
		
		retSeries = new GraphViewSeries(graphData);
		trackingDB.close();
		maxStorage = Environment.getExternalStorageDirectory().getTotalSpace();
		if ((maxUsage / maxStorage) < 0.7) {
			maxStorage = maxUsage;
			lowerThanMax = true;
		} else {
			lowerThanMax = false;
		}
		
		return retSeries;
	}
}
