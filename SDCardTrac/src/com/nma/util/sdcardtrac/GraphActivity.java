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
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

public class GraphActivity extends Activity {
	DatabaseManager trackingDB;
	
	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		setContentView(R.layout.graph);
		
		// Get data from DB
		trackingDB = new DatabaseManager(this);
		GraphViewSeries storageGraphData = getData();
		// Plot it
		GraphView storageGraph = new LineGraphView(this, "Storage History") {
			@Override  
			protected String formatLabel(double value, boolean isValueX) {  
				if (isValueX) {  
					// convert unix time to human time  
					return DateFormat.getDateInstance().format(new Date((long) value*1000));  
				} else return super.formatLabel(value, isValueX); // let the y-value be normal-formatted  
			}
		};
		
		storageGraph.addSeries(storageGraphData);
		((LinearLayout)findViewById(R.id.graph_layout)).addView(storageGraph);
	}
	
	// Helpers
	private GraphViewSeries getData() {
		trackingDB.openToRead();
		// TODO: use start/end time hooks
		ArrayList <ContentValues> dbData = (ArrayList<ContentValues>)trackingDB.getValues(0, 0);
		
		// Build the graph
		GraphViewSeries retSeries;
		Log.d(getClass().getName(), "Creating data of len " + dbData.size());
		GraphViewData [] graphData = new GraphViewData[dbData.size()];
		int i = 0;
		for (ContentValues d : dbData) {
			String timeStamp = (String)d.get(DatabaseManager.ID_COLUMN);
			String usage = (String)d.get(DatabaseManager.DELTA_COLUMN);
			String changeLog = (String)d.get(DatabaseManager.LOG_COLUMN); // TODO:
			graphData[i] = new GraphViewData(Long.parseLong(timeStamp), Integer.parseInt(usage));
			Log.d(getClass().getName(), "Added to graph : " + Long.parseLong(timeStamp)
					+ " - " + Integer.parseInt(usage));
		}
		
		retSeries = new GraphViewSeries(graphData);
		trackingDB.close();
		return retSeries;
	}
}
