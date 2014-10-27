package com.nma.sdcardtrac;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

/**
 * Created by naren on 7/9/14.
 */
public class GraphTabListener implements ActionBar.TabListener {
    private Fragment mFragment;
    private final GraphActivity mActivity;
    private final String mTag;

    // Constructor
    public GraphTabListener(Activity activity, String tag) {
        mActivity = (GraphActivity)activity;
        mTag = tag;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // Check if the fragment is already initialized
        if (mFragment == null) {
            // If not, instantiate and add it to the activity
            mFragment = GraphFragment.newInstance(mTag, mActivity.getTimeInterval());
            Log.d(getClass().getName(), "Created " + mFragment);
            ft.replace(R.id.graph_layout, mFragment, mTag);
        } else {
            Log.d(getClass().getName(), "Attach " + mFragment.isDetached() + ":" + mFragment);
            if (mFragment.isDetached())
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (mFragment != null) {
            Log.d(getClass().getName(), "Detach " + mFragment);
            // Detach the fragment, because another one is being attached
            ft.detach(mFragment);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }
}
