package com.creativakids.alexambot.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.creativakids.alexambot.R;

/**
 * Dialog for the display of a connection error
 *
 * Created by Mauricio Lara on 7/5/17.
 */
public class ConnectionErrorDialogFragment extends AppCompatDialogFragment implements View.OnClickListener{

    // Reference for the calling callback
    private Callback callback;

    /**
     * Static factory method for the dialog
     * */
    public static ConnectionErrorDialogFragment newInstance(){
        // Creating and returning the fragment with arguments
        ConnectionErrorDialogFragment connectionConfirmationDialogFragment =
                new ConnectionErrorDialogFragment();
        return connectionConfirmationDialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Getting the reference for the calling context callback
        try{
            callback = (Callback) context;
        }catch ( ClassCastException e ){
            throw new IllegalStateException("Calling context: " + context.getClass().getSimpleName()
                    + " must implement " + Callback.class.getSimpleName() );
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We generate the view for the interface
        View dialogView = inflater.inflate( R.layout.dialog_connection_error, container, false /* attachToRoot */);

        // Setting the click listeners for the view actions
        dialogView.findViewById(R.id.bttn_ok).setOnClickListener(
                ConnectionErrorDialogFragment.this /* View.OnClickListener */);

        return dialogView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();

        // Avoid memory leak
        callback = null;
    }

    /**
     * Interface for the device connections
     * */
    public interface Callback{
        void onDialogCancelled();
    }

    /* -- Callbacks for the onClickListener interface -- */
    @Override
    public void onClick(View v) {
        // We just handle the ok click to dismiss
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if( callback != null ){
            callback.onDialogCancelled();
        }
    }
}
