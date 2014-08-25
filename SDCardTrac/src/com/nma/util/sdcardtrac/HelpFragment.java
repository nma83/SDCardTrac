package com.nma.util.sdcardtrac;



import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HelpFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class HelpFragment extends DialogFragment
    implements DialogInterface.OnClickListener {
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment HelpFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HelpFragment newInstance() {
        HelpFragment fragment = new HelpFragment();
        return fragment;
    }

    public HelpFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
/*        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Help");
        return dialog;*/
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.help)
                .setMessage(R.string.help_text)
                .setNeutralButton(R.string.back, this);

        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
/*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View retView = super.onCreateView(inflater, container, savedInstanceState);
        TextView help = (TextView)retView.findViewById(R.id.help_textview);
        help.setMovementMethod(LinkMovementMethod.getInstance());
        return retView;
    }
*/
    @Override
    public void onClick(DialogInterface dialog, int which) {
        // Empty
    }
}
