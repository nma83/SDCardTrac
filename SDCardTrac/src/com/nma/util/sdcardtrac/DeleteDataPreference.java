package com.nma.util.sdcardtrac;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

/**
 * Created by naren on 10/14/14.
 */
// Custom preference for deleting data
public class DeleteDataPreference extends DialogPreference {
    RadioGroup grp;
    String lead;

    public DeleteDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.delete_select);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogTitle(R.string.delete_title);
        setDialogIcon(null);
        lead = "";
    }

    @Override
    protected void onBindDialogView(View v) {
        grp = (RadioGroup)v.findViewById(R.id.delete_group);
        super.onBindDialogView(v);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        int deleteBefore;
        String storeVal = getPersistedString("0");
        // Get selected value
        int id = grp.getCheckedRadioButtonId();
        deleteBefore = 60 * 60 * 24; // Day

        switch (id) {
            case R.id.delete_now:
                deleteBefore = 0;
                break;
            case R.id.delete_week:
                deleteBefore *= 7;
                break;
            case R.id.delete_month:
                deleteBefore *= 30;
                break;
            case R.id.delete_year:
                deleteBefore *= 365;
                break;
            default:
                deleteBefore = 0;
                break;
        }

        //Log.d(getClass().getName(), "Dialog = " + deleteBefore + " from " + id);
        if (positiveResult && (id != -1)) {
            String toStore = Integer.toString(deleteBefore);
            if (toStore.equals(storeVal)) {
                toStore = "0" + toStore; // Fake change
            }
            persistString(toStore);
        }
    }
}
