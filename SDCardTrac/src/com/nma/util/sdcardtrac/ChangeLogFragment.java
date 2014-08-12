package com.nma.util.sdcardtrac;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.Arrays;


public class ChangeLogFragment extends DialogFragment
    implements DialogInterface.OnClickListener{
    public static final String CHANGELOG_FRAG_MSGS_ARG = "logMsgs";

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

        if (logMsgs.length == 0) {
            logMsgs = new CharSequence[2];
            logMsgs[0] = "No Data";
            logMsgs[1] = "";
        }

        String title = (String)logMsgs[0];
        logMsgs = Arrays.copyOfRange(logMsgs, 1, logMsgs.length);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title) //R.string.changelog_dialog_title)
                .setItems(logMsgs, this)
                .setNeutralButton(R.string.changelog_dialog_button, this);

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Empty
    }
}
