package com.nma.sdcardtrac;

import android.app.Service;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.text.TextUtils;
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
    private static int TEXT_MARQUEE_REPEAT = 3;
    private TextUtils.TruncateAt textViewMode;

    public MyExpandableListAdapter(Context c, boolean marqueMode) {
        groups = new ArrayList();
        children = new ArrayList();
        textViewMode = (marqueMode) ? TextUtils.TruncateAt.MARQUEE
                : TextUtils.TruncateAt.END;
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

    public void addChild(int group, String childName) {
        // Add to last created group
        children.get(group).add(childName);
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
        TextView textView;

        paddingLeft = ctx.getResources().getDisplayMetrics().density;
        paddingLeft *= 40.0;
        paddingLeft += 0.5;
        paddingInt = (int)paddingLeft;

        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Service.LAYOUT_INFLATER_SERVICE);

        if (textViewMode == TextUtils.TruncateAt.MARQUEE) {
            textView = (TextView) inflater.inflate(R.layout.marquee_textview, null);
        } else {
            textView = new TextView(ctx);
            textView.setLayoutParams(lp);
            textView.setGravity(Gravity.LEFT);
            textView.setSingleLine(false);
        }

        // Set the text starting position
        textView.setPadding(paddingInt, 0, 0, 0);
        //textView.setOnClickListener(this);
        return textView;
    }

    public void makeMarquee(TextView textView) {
        // All this circus to get marquee
        textView.setEllipsize(textViewMode);
        if (textViewMode == TextUtils.TruncateAt.MARQUEE) {
            textView.setMarqueeRepeatLimit(TEXT_MARQUEE_REPEAT);
            textView.setSingleLine();
            textView.setSelected(true);
            textView.setFocusable(false);
            textView.setFocusableInTouchMode(false);
            textView.setHorizontallyScrolling(true);
        } else {
            //textView.setMaxLines(2);
            textView.setHorizontallyScrolling(false);
        }
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        TextView textView = getGenericView();
        textView.setText(getChild(groupPosition, childPosition).toString());
        makeMarquee(textView);
        return textView;
    }

    public Object getGroup(int groupPosition) {
        String ret = groups.get(groupPosition) + " (" + getChildrenCount(groupPosition) + ")";
        return ret;
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

        //textView.setGravity(Gravity.LEFT);
        textView.setText(getGroup(groupPosition).toString());
        makeMarquee(textView);
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
