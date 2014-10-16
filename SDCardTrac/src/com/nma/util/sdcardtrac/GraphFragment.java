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

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.lang.Math;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GraphFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GraphFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */

public class GraphFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<List<DatabaseLoader.DatabaseRow>>, GraphView.GraphSelectHandler {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_STORAGE_TYPE = "storage_type";
    public static final String ARG_TIME_INTERVAL = "time_interval";

    // TODO: Rename and change types of parameters
    private String storageType;
    private String timeInterval;

    private OnFragmentInteractionListener mListener;
    private List<DatabaseLoader.DatabaseRow> locData;
    private String logMessages[];
    private GraphView storageGraph;
    private GraphViewSeries graphSeries;
    private String graphLabel;
    private long maxStorage, startTime, endTime, viewPortStart, viewPortWidth;
    private ProgressDialog loadingDBDialog;

    private static final float GRAPHVIEW_TEXT_SIZE_DIP = 8.0f;
    private static final float GRAPHVIEW_POINT_SIZE_DIP = 4.0f;
    private static boolean firstView = true;
    private boolean secondSelect = false;
    private int selectedPoint = 0;
    private boolean redraw = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GraphFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GraphFragment newInstance(String param1, String param2) {
        GraphFragment fragment = new GraphFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORAGE_TYPE, param1);
        args.putString(ARG_TIME_INTERVAL, param2);
        fragment.setArguments(args);

        return fragment;
    }

    public GraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	redraw = false;
        if (getArguments() != null) {
            storageType = getArguments().getString(ARG_STORAGE_TYPE);
            timeInterval = getArguments().getString(ARG_TIME_INTERVAL);
            Log.d("GraphFragOnCreate", "Got args " + storageType + ", " + this);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

	if (savedInstanceState != null) {
	    viewPortStart = savedInstanceState.getLong("viewportstart");
	    viewPortWidth = savedInstanceState.getLong("viewportwidth");
	    redraw = true;
	}

        // Load data
        getLoaderManager().initLoader(0, null, this);
        //Log.d("Fragment:Activity", "Done with initLoader");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        // Inflate the layout for this fragment
        View retView = inflater.inflate(R.layout.fragment_graph, container, false);

        return retView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String reason) {
        if (mListener != null) {
            mListener.onFragmentInteraction(reason);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        //Log.d("FragLoader", "Returning created loader");
        getActivity().setProgressBarIndeterminateVisibility(true);
        return new DatabaseLoader(getActivity(), storageType);
    }

    @Override
    public void onLoadFinished(Loader<List<DatabaseLoader.DatabaseRow>> loader,
                               List<DatabaseLoader.DatabaseRow> data) {
	if (SettingsActivity.ENABLE_DEBUG)
	    Log.d("FragLoader", "Done loading with " + data.size() + " items");

        locData = data;
        getActivity().setProgressBarIndeterminateVisibility(false);

        if (locData.size() <= 1) {
            // Flag an error
            Toast.makeText(getActivity(), R.string.db_empty_error, Toast.LENGTH_SHORT).show();
            // Goto settings
            if (firstView && mListener != null) {
                firstView = false;
                mListener.onFragmentInteraction(getResources().getString(R.string.act_goto_settings));
            }
        } else {
            createGraphData();

            if (graphSeries != null)
                drawGraph(((LinearLayout)getView().findViewById(R.id.graph_fragment_layout)), false);
            else
                Toast.makeText(getActivity(), R.string.db_empty_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
	//if (getActivity() != null)
	//    getActivity().setProgressBarIndeterminateVisibility(true);
        locData = null;
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        if (graphSeries != null) {
	    // Restore state
	    if (savedInstanceState != null) {
		viewPortStart = savedInstanceState.getLong("viewportstart");
		viewPortWidth = savedInstanceState.getLong("viewportwidth");
		redraw = true;
	    }
            drawGraph((LinearLayout)view.findViewById(R.id.graph_fragment), redraw);
	}
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("viewportstart", viewPortStart);
	outState.putLong("viewportwidth", viewPortWidth);
    }

    // Create the graph plot
    private void createGraphData() {
        int i = 0;
        long maxUsage = 0, timeStamp;

	if (locData == null) {
	    Log.e(getClass().getName(), "No data created by loader!");
	    return;
	}

        // Build the graph
	if (SettingsActivity.ENABLE_DEBUG)
	    Log.d(getClass().getName(), "Creating data of len " + locData.size());

        GraphView.GraphViewData[] graphData = new GraphView.GraphViewData[1];
        ArrayList <GraphView.GraphViewData> dataArr = new ArrayList<GraphView.GraphViewData>();
        logMessages = new String[locData.size()];

        if (locData.size() > 0)
            startTime = locData.get(0).getTime();
        else
            startTime = 0;

	viewPortStart = startTime;
        for (DatabaseLoader.DatabaseRow row : locData) {
            timeStamp = row.getTime();
            endTime = timeStamp;
            long usage = Math.abs(row.getUsage());
            //graphData[i] = new GraphView.GraphViewData(timeStamp, usage);
            if (row.getChangeLog().contains("/")) // Add only paths
                dataArr.add(new GraphView.GraphViewData(timeStamp, usage));
            else
                continue;

            if (usage > maxUsage)
                maxUsage = usage;

            // Log
            logMessages[i] = row.getChangeLog();
            i++;
        }

        graphData = dataArr.toArray(graphData);

	if (SettingsActivity.ENABLE_DEBUG)
	    Log.d("createGraphData", "Creating graph data len " + dataArr.size() + " max " + maxUsage);

        if (dataArr.size() == 0) {
            graphSeries = null;
            return;
        }

        graphSeries = new GraphViewSeries(graphData);
        maxStorage = Environment.getExternalStorageDirectory().getTotalSpace();

        //String usageRatioInt = new DecimalFormat("#.#").format((maxUsage * 100f) / maxStorage);
        //usageRatio = DatabaseLoader.convertToStorageUnits(maxUsage) + " max used ("
        //        + usageRatioInt + "%) of " + DatabaseLoader.convertToStorageUnits(maxStorage);
        graphLabel = Environment.getExternalStorageDirectory().getAbsolutePath() +
        	" - " + DatabaseLoader.convertToStorageUnits(maxStorage);

        // Round up to GB
        if (maxStorage * 0.7 > maxUsage)
            maxStorage = (long)Math.ceil((double)maxUsage / 1000000000) * 1000000000;
        else
            maxStorage = (long)Math.ceil((double)maxStorage / 1000000000) * 1000000000;
    }

    private void drawGraph(LinearLayout view, boolean redraw) {
        float textSize, dispScale, pointSize;

        // Determine text size
        dispScale = getActivity().getResources().getDisplayMetrics().density;
        textSize = (GRAPHVIEW_TEXT_SIZE_DIP * dispScale) + 0.5f;
        pointSize = (GRAPHVIEW_POINT_SIZE_DIP * dispScale) + 0.5f;

        storageGraph = new LineGraphView(getActivity(), graphLabel);
        storageGraph.setCustomLabelFormatter(new CustomLabelFormatter() {
            String prevDate = "";

            @Override
            public String formatLabel(double value, boolean isValueX, int index, int lastIndex) {
                String retValue;
                boolean valueXinRange;

                valueXinRange = (index == 0) || (index == lastIndex);
                if (isValueX) { // Format time in human readable form
                    if (valueXinRange) {
                        String dateStr;
                        Date currDate = new Date((long) value);

                        dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(currDate);

                        if (dateStr.equals(prevDate)) {
                            // Show hh:mm
                            retValue = DateFormat.getTimeInstance(DateFormat.SHORT).format(currDate);
                        } else {
                            retValue = dateStr;
                        }

                        prevDate = dateStr;
                        //Log.d(getClass().getName(), "Label is : " + retValue);
                    } else {
                        retValue = " ";
                    }
                } else { // Format size in human readable form
                    retValue = DatabaseLoader.convertToStorageUnits(value);
                    //prevDate = "";
                }
                //return super.formatLabel(value, isValueX); // let the y-value be normal-formatted
                return retValue;
            }
        });

        storageGraph.addSeries(graphSeries);
        storageGraph.setManualYAxis(true);
        storageGraph.setManualYAxisBounds(maxStorage, 0);
        storageGraph.setScalable(false);
        //storageGraph.setScrollable(true);
        storageGraph.getGraphViewStyle().setGridColor(Color.GREEN);
        storageGraph.getGraphViewStyle().setHorizontalLabelsColor(Color.YELLOW);
        storageGraph.getGraphViewStyle().setVerticalLabelsColor(Color.RED);
        storageGraph.getGraphViewStyle().setTextSize(textSize);
        storageGraph.getGraphViewStyle().setNumHorizontalLabels(2);
        storageGraph.getGraphViewStyle().setNumVerticalLabels(5);
        storageGraph.getGraphViewStyle().setVerticalLabelsWidth((int)(textSize * 4));
        //storageGraph.setMultiLineXLabel(true, ";");
        ((LineGraphView)storageGraph).setDrawBackground(true);
        ((LineGraphView)storageGraph).setDrawDataPoints(true);
        ((LineGraphView)storageGraph).setDataPointsRadius(pointSize);
        //storageGraph.highlightSample(0, true, locData.size() - 1);
        // Add selector callback
        storageGraph.setSelectHandler(this);

        setViewport(redraw);
        if (view != null)
            view.addView(storageGraph);
        if (SettingsActivity.ENABLE_DEBUG)
            Log.d(getClass().getName(), "Drew the graph, redraw=" + redraw);
    }

    // Helper to manipulate graph viewport
    public void setViewport(boolean redraw) {
        Calendar calcView[];

        // Start time
        calcView = new Calendar[2];
        calcView[0] = Calendar.getInstance();
        calcView[0].setTimeInMillis(viewPortStart);
        calcView[1] = (Calendar)calcView[0].clone();

        if (timeInterval == null)
            timeInterval = "Day";

        // Override with setting
        if (timeInterval.equals("Hour"))
            calcView[1].add(Calendar.HOUR_OF_DAY, -1);
        else if (timeInterval.equals("Day"))
            calcView[1].add(Calendar.DAY_OF_MONTH, -1);
        else if (timeInterval.equals("Week"))
            calcView[1].add(Calendar.WEEK_OF_YEAR, -1);
        else if (timeInterval.equals("Month"))
            calcView[1].add(Calendar.MONTH, -1);
        else
            calcView[1].add(Calendar.YEAR, -1);

        viewPortWidth = calcView[0].getTimeInMillis() - calcView[1].getTimeInMillis();
        //Calendar.getInstance().getTimeInMillis() - calcView.getTimeInMillis();
        secondSelect = false;
        if (storageGraph != null) {
            storageGraph.setViewPort(viewPortStart, viewPortWidth);
            if (viewPortWidth > (endTime - startTime))
                storageGraph.setScrollable(false);
            else {
                storageGraph.setScrollable(true);
		if (!redraw)
		    storageGraph.scrollToEnd();
            }
            Log.d("GraphFrag", "Updated viewport to " + timeInterval + ", " + viewPortStart +
                    " - " + viewPortWidth);
        }
    }

    public void setTimeInterval(String interval, boolean visible) {
        timeInterval = interval;

        if (visible) {
            Log.d("GraphFrag", "Refreshing fragment " + this);
            // Option 1: let the activity refresh the fragment
            mListener.onFragmentInteraction(storageType);
        }
        // Option 2: do it here
        //getLoaderManager().restartLoader(0, null, this);
    }

    public void restartLoader() {
        Log.d(getClass().getName(), "Restarting loader in " + storageType);
        getLoaderManager().restartLoader(0, null, this);
    }

    // Graph select handler
    @Override
    public void onGraphSelect(int i, int start) {
        String [] logLines;

	if (logMessages == null) {
	    Log.e(getClass().getName(), "No log messages to display!");
	    return;
	}

        if (logMessages.length == 0) {
            logLines = new String[2];
            logLines[0] = "No Data";
            logLines[1] = "-";
        } else {
            int showInd = i;

            if (i > 0 && start > 0) showInd -= 1;
            //Log.d(getClass().getName(), "Selected index[" + i + "] = " + logMessages[i]);
            // Split each line of the log
            logLines = logMessages[showInd].split("\n");
        }

        if (secondSelect == false && i == selectedPoint)
            secondSelect = true;
        if (SettingsActivity.ENABLE_DEBUG)
            Log.d(getClass().getName(), "onSelect " + i + ", selected " + selectedPoint + ", starting " + start);

        int selInd = (i - start);
        if (start > 0) selInd += 1;

        storageGraph.highlightSample(0, true, selInd);
        if (secondSelect) {
            // Call dialog
            ChangeLogFragment dialog = ChangeLogFragment.newInstance(logLines);
            dialog.show(getActivity().getSupportFragmentManager(), "dialog");
        }

        selectedPoint = i;
        secondSelect = false;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String reason);
    }

}
