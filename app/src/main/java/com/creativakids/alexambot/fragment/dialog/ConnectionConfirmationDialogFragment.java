package com.creativakids.alexambot.fragment.dialog;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creativakids.alexambot.R;

/**
 * Simple dialog for the confirmation of the connection with a device
 *
 * Created by Mauricio Lara on 7/3/17.
 */
public class ConnectionConfirmationDialogFragment extends AppCompatDialogFragment implements View.OnClickListener{

    // Reference for the arguments
    private static final String FRAG_ARG_BLUETOOTH_DEVICE = "FRAG_ARG_BLUETOOTH_DEVICE";

    // View reference
    private TextView txtConfirmation;

    // Reference for the calling callback
    private Callback callback;

    // Reference for the bluetooth device
    private BluetoothDevice device;

    /**
     * Static factory method for the dialog
     * */
    public static ConnectionConfirmationDialogFragment newInstance( BluetoothDevice device ){
        // Creating the fragment arguments
        Bundle args = new Bundle();
        args.putParcelable( FRAG_ARG_BLUETOOTH_DEVICE, device );

        // Creating and returning the fragment with arguments
        ConnectionConfirmationDialogFragment connectionConfirmationDialogFragment =
                new ConnectionConfirmationDialogFragment();
        connectionConfirmationDialogFragment.setArguments( args );
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


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Getting the reference for the bluetooth device
        device = getFragArgBluetoothDevice();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We generate the view for the interface
        View dialogView = inflater.inflate( R.layout.dialog_connection_confirmation, container, false /* attachToRoot */);

        // Setting the click listeners for the view actions
        dialogView.findViewById(R.id.bttn_cancel).setOnClickListener(
                ConnectionConfirmationDialogFragment.this /* View.OnClickListener */);
        dialogView.findViewById(R.id.bttn_connect ).setOnClickListener(
                ConnectionConfirmationDialogFragment.this /* View.OnClickListener */);
        txtConfirmation = (TextView) dialogView.findViewById(R.id.txt_linking_device);

        return dialogView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Setting the text for the confirmation
        txtConfirmation.setText( getString(R.string.connection_confirmation_message, device.getName()));
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
     * Obtains the component of the bluetooth device to resumeConnection via args
     * */
    private BluetoothDevice getFragArgBluetoothDevice(){
        if( getArguments() == null ){
            throw new IllegalStateException("You need to provide a bluetooth device via argument: "
                    + FRAG_ARG_BLUETOOTH_DEVICE );
        }else{
            BluetoothDevice device = getArguments().getParcelable( FRAG_ARG_BLUETOOTH_DEVICE );
            if( device == null ){
                throw new IllegalStateException("You need to provide a bluetooth device via argument: "
                        + FRAG_ARG_BLUETOOTH_DEVICE );
            }else{
                return device;
            }
        }
    }

    /**
     * Interface for the device connections
     * */
    public interface Callback{
        void onConnectToDevice( BluetoothDevice bluetoothDevice );
    }

    /* -- Callbacks for the onClickListener interface -- */
    @Override
    public void onClick(View v) {
        switch ( v.getId() ){
            case R.id.bttn_connect:
                if( callback != null ){
                    callback.onConnectToDevice( device );
                }

                /* falls through */
            case R.id.bttn_cancel:
            default:
                dismiss();
        }
    }
}
