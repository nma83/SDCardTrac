package com.nma.util.sdcardtrac;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ExpandableListView;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.BaseExpandableListAdapter;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

// Adapter for expandable list view
public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    // Sample data set.  children[i] contains the children (String[]) for groups[i].
    private List<String> groups;
    private List<List<String>> children;
    private boolean noItems = false;
    private Context ctx;
        
    public MyExpandableListAdapter(Context c) {
	groups = new ArrayList();
	children = new ArrayList();
	ctx = c;
    }

    public void addGroup(String groupName) {
	groups.add(groupName);
	children.add(new ArrayList<String>());
    }

    public void addChild(String childName) {
	// Add to last created group
	children.get(children.size() - 1).add(childName);
    }

    public Object getChild(int groupPosition, int childPosition) {
	return children.get(groupPosition).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
	return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
	return children.get(groupPosition).size();
    }

    public TextView getGenericView() {
	float paddingLeft;
	int paddingInt;
	paddingLeft = ctx.getResources().getDisplayMetrics().density;
	paddingLeft *= 40.0;
	paddingLeft += 0.5;
	paddingInt = (int)paddingLeft;
	// Layout parameters for the ExpandableListView
	AbsListView.LayoutParams lp = new AbsListView.LayoutParams
	    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

	TextView textView = new TextView(ctx);
	textView.setLayoutParams(lp);
	// Center the text vertically
	textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
	// Set the text starting position
	textView.setPadding(paddingInt, 0, 0, 0);
	return textView;
    }
        
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			     View convertView, ViewGroup parent) {
	TextView textView = getGenericView();

	textView.setText(getChild(groupPosition, childPosition).toString());
	return textView;
    }

    public Object getGroup(int groupPosition) {
	return (groups.get(groupPosition) + " (" + getChildrenCount(groupPosition) + ")");
    }

    public int getGroupCount() {
	return groups.size();
    }

    public long getGroupId(int groupPosition) {
	return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			     ViewGroup parent) {
	TextView textView = getGenericView();
	String str = (String)getGroup(groupPosition);

	if (groupPosition == 0)
	    noItems = str.equals("No files detected");

	textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
	textView.setText(getGroup(groupPosition).toString());
	return textView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
	return true;
    }

    public boolean hasStableIds() {
	return true;
    }

    public boolean isEmpty() {
	return noItems;
    }

    public void clear() {
	int i = 0;
	for (String s : groups) {
	    children.get(i++).clear();
	}
	children.clear();
	groups.clear();
    }
}
