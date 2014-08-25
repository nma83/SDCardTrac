package com.nma.util.sdcardtrac;

import android.app.Dialog;
import android.graphics.Color;
import android.support.v4.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ChangeLogFragment extends DialogFragment
    implements DialogInterface.OnClickListener {
    public static final String CHANGELOG_FRAG_MSGS_ARG = "logMsgs";
    public static final String CHANGELOG_FRAG_HEAD_ARG = "logHead";
    public static final int NUM_CHANGELOG_VIEWS = 3; // Created, deleted, modified
    public static final int CHANGELOG_VIEW_CREATED = 0;
    public static final int CHANGELOG_VIEW_DELETED = 1;
    public static final int CHANGELOG_VIEW_MODIFED = 2;
    public static String [] CHANGELOG_HEADINGS = {
            "Files created",
            "Files deleted",
            "Files modified"
    };
    public static int [] CHANGELOG_COLOURS = {
            Color.parseColor("#008000"),
            Color.parseColor("#800000"),
            Color.parseColor("#999900")
    };

    private ArrayList<ArrayList<String>> changeList;

    public static ChangeLogFragment newInstance(String [] logMsgs) {
        ChangeLogFragment frag = new ChangeLogFragment();
        Bundle args = new Bundle();
        args.putStringArray(CHANGELOG_FRAG_MSGS_ARG, logMsgs);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get list of messages
        CharSequence [] logMsgs = getArguments().getStringArray(CHANGELOG_FRAG_MSGS_ARG);
        changeList = new ArrayList<ArrayList<String>>();

        for (int i = 0; i < NUM_CHANGELOG_VIEWS; i++) {
            changeList.add(new ArrayList<String>());
        }

        if (logMsgs.length == 0) {
            logMsgs = new CharSequence[2];
            logMsgs[0] = "No Data";
            logMsgs[1] = "";
        }

        String title = (String)logMsgs[0];
        logMsgs = Arrays.copyOfRange(logMsgs, 1, logMsgs.length);
/*
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title) //R.string.changelog_dialog_title)
                .setItems(logMsgs, this)
                .setNeutralButton(R.string.changelog_dialog_button, this);
*/

        // Split lists
        for (CharSequence i : logMsgs) {
            String strI = i.toString();
            String [] split = strI.split(":");
            String file;

            if (split.length > 1) {
                file = split[1];
            } else {
                file = split[0];
            }

            //Log.d("tmp", "Processing " + strI);
            if (strI.startsWith("C") || strI.startsWith("V")) {
                changeList.get(CHANGELOG_VIEW_CREATED).add(file);
            } else if (strI.startsWith("D")) {
                changeList.get(CHANGELOG_VIEW_DELETED).add(file);
            } else { //if (strI.startsWith("M")) {
                changeList.get(CHANGELOG_VIEW_MODIFED).add(file);
            }
        }

        for (ArrayList<String> i : changeList) {
            if (i.size() == 0) {
                i.add("No files detected");
            }
        }

        //Log.d("tmp", "Changelist created " + changeList);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //dialog.getWindow() .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setTitle(title);
        return dialog;
        //return builder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_log, container);
        ChangeLogAdapter tabAdapter = new ChangeLogAdapter(getChildFragmentManager());
        ViewPager viewPager = (ViewPager)view.findViewById(R.id.changelog_pager);
        viewPager.setAdapter(tabAdapter);

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Empty
    }

    // Classes to view log
    public static class ChangeLogDetail extends Fragment {
        public static ChangeLogDetail newInstance(int position, String [] msgs) {
            ChangeLogDetail frag = new ChangeLogDetail();
            Bundle args = new Bundle();
            args.putStringArray(CHANGELOG_FRAG_MSGS_ARG, msgs);
            args.putInt(CHANGELOG_FRAG_HEAD_ARG, position);
            frag.setArguments(args);
            return frag;
        }

        // Populate list
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.view_changelog, container, false);
            ListView listView = (ListView)root.findViewById(R.id.changelog_list);
            TextView textView = (TextView)root.findViewById(R.id.changelog_heading);
            LinearLayout layout = (LinearLayout)root.findViewById(R.id.changelog_layout);

            // Make the list
            String [] listItems = getArguments().getStringArray(CHANGELOG_FRAG_MSGS_ARG);
            int position = getArguments().getInt(CHANGELOG_FRAG_HEAD_ARG);
            textView.setText(CHANGELOG_HEADINGS[position]);
            layout.setBackgroundColor(CHANGELOG_COLOURS[position]);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, listItems);
            listView.setAdapter(adapter);

            return root;
        }
    }

    private class ChangeLogAdapter extends FragmentPagerAdapter {

        public ChangeLogAdapter(FragmentManager fragMan) {
            super(fragMan);
        }

        @Override
        public Fragment getItem(int position) {
            if (position < NUM_CHANGELOG_VIEWS) {
                String [] tmpList = new String[changeList.get(position).size()];
                changeList.get(position).toArray(tmpList);
                return ChangeLogDetail.newInstance(position, tmpList);
            } else
                return null;
        }

        @Override
        public int getCount() {
            return NUM_CHANGELOG_VIEWS;
        }
    }
}
