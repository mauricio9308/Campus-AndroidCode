package com.creativakids.alexambot.activity;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.creativakids.alexambot.R;
import com.creativakids.alexambot.adapter.CommandsAdapter;
import com.creativakids.alexambot.fragment.dialog.ConnectionErrorDialogFragment;
import com.creativakids.alexambot.fragment.dialog.ProgressDialogFragment;
import com.creativakids.alexambot.fragment.dialog.VoiceCommandDialogFragment;
import com.creativakids.alexambot.view.SimpleDividerItemDecoration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Activity for the control handling of the mBot
 *
 * Created by Mauricio Lara on 7/3/17.
 */
public class ControlActivity extends AppCompatActivity implements View.OnClickListener, ConnectionErrorDialogFragment.Callback,
    VoiceCommandDialogFragment.CommandCallback{

    // Logging tag
    private static final String LOG_TAG = ControlActivity.class.getSimpleName();

    // Extra args for the bluetooth connection
    public static final String EXTRA_ARG_BLUETOOTH_DEVICE = "EXTRA_ARG_BLUETOOTH_DEVICE";

    // State preservation tags
    private static final String STATE_BLUETOOTH_DEVICE = "STATE_BLUETOOTH_DEVICE";

    // Reference for the request codes
    private static final int REQUEST_CODE_RECORD_AUDIO = 100;

    // Frag tags
    private static final String FRAG_TAG_LEX_INPUT = "FRAG_TAG_LEX_INPUT";
    private static final String FRAG_TAG_PROGRESS_DIALOG = "FRAG_TAG_PROGRESS_DIALOG";
    private static final String FRAG_TAG_CONNECTION_ERROR_DIALOG = "FRAG_TAG_CONNECTION_ERROR_DIALOG";

    // Reference for the device
    private BluetoothDevice device;

    // Reference for the connection state
    private BluetoothSocket bTSocket;
    private OutputStream connectionStream;

    // Reference for the commands to the mBot
    public static final int COMMAND_FORWARD = 1;
    public static final int COMMAND_LEFT = 2;
    public static final int COMMAND_RIGHT = 3;
    public static final int COMMAND_REVERSE = 4;
    public static final int COMMAND_TURN = 5;
    public static final int COMMAND_STOP = 6;
    public static final int COMMAND_B_BUTTON = 7;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting the view for the activity
        setContentView( R.layout.activity_control );

        // Getting the reference for the device to resumeConnection
        if( savedInstanceState == null ){
            device = getExtraDeviceToConnect();
        }else{
            device = savedInstanceState.getParcelable( STATE_BLUETOOTH_DEVICE );
        }

        // Setting the subtitle
        ActionBar actionBar = getSupportActionBar();
        if( actionBar != null ){
            actionBar.setSubtitle( device.getName() );
        }

        // Setting up the control actions
        ArrayList<String> commands = new ArrayList<>();
        commands.add("forward");
        commands.add("reverse");
        commands.add("left");
        commands.add("right");
        commands.add("stop");
        commands.add("dance");
        commands.add("dance dance");

        // Setting up the adapter for the recycler of commands
        CommandsAdapter commandsAdapter = new CommandsAdapter( ControlActivity.this /* context */, commands );
        RecyclerView commandsRecycler = (RecyclerView) findViewById(R.id.commandList);
        commandsRecycler.addItemDecoration( new SimpleDividerItemDecoration( ControlActivity.this /* context */));
        commandsRecycler.setLayoutManager( new LinearLayoutManager( ControlActivity.this /* context */));
        commandsRecycler.setAdapter( commandsAdapter );

        // Registering the click events
        findViewById(R.id.bttn_a).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_b).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_cross_bottom).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_cross_left).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_cross_right).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_cross_top).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.bttn_trigger_microphone).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
        findViewById(R.id.view_cross_center).setOnClickListener( ControlActivity.this /* View.OnClickListener */);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // We start the bluetooth connection
        resumeConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Closing the current connection
        closeConnection();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // We preserve the instance for the device
        if( device != null ){
            outState.putParcelable( STATE_BLUETOOTH_DEVICE, device );
        }
    }

    @Override
    public void onClick(View v) {
        switch ( v.getId() ){
            case R.id.bttn_cross_bottom:
            case R.id.bttn_cross_left:
            case R.id.bttn_cross_right:
            case R.id.bttn_cross_top:
                handleCrossUpdate( v.getId() );
                break;
            case R.id.bttn_trigger_microphone:
                // Opens the microphone interface for the device
                openMicrophoneDialog();
                break;
            case R.id.bttn_a:
                handleAButton();
                break;
            case R.id.bttn_b:
                handleBButton();
                break;
            case R.id.view_cross_center:
                sendCommand( COMMAND_STOP );
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // We handle the result of the data loading by showing an action dialog
        AlertDialog.Builder confirmDownloadDialog = new AlertDialog.Builder( ControlActivity.this /* context */)
                .setCancelable(true)
                .setTitle(R.string.close_connection_title)
                .setMessage(R.string.close_connection_message)
                .setNegativeButton( android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // We do nothing...
                    }
                }).setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // We continue with the flow
                        ControlActivity.super.onBackPressed();
                    }
                });
        AlertDialog confirmDialog = confirmDownloadDialog.create();
        confirmDialog.show();
    }

    /**
     * Handling of the cross inputs for the device
     * */
    public void handleCrossUpdate( int crossButtonReference ){
        switch ( crossButtonReference ){
            case R.id.bttn_cross_bottom:
                sendCommand( COMMAND_REVERSE );
                break;
            case R.id.bttn_cross_left:
                sendCommand( COMMAND_LEFT );
                break;
            case R.id.bttn_cross_right:
                sendCommand( COMMAND_RIGHT );
                break;
            case R.id.bttn_cross_top:
                sendCommand( COMMAND_FORWARD );
                break;
        }
    }

    /**
     * Handles the click of the A button
     * */
    private void handleAButton(){
        sendCommand( COMMAND_TURN );
    }

    /**
     * Handles the click of the B button
     * */
    private void handleBButton(){
        sendCommand( COMMAND_B_BUTTON );
    }

    /**
     * Opens the microphone dialog for the lex input
    * */
    private void openMicrophoneDialog(){
        // We check if we have the permissions
        boolean havePermissions = ContextCompat.checkSelfPermission(ControlActivity.this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if( havePermissions ){
            // We just open the dialog
            VoiceCommandDialogFragment commandDialogFragment = VoiceCommandDialogFragment.newInstance();
            commandDialogFragment.show( getSupportFragmentManager(), FRAG_TAG_LEX_INPUT );
        }else{
            // We request the permissions
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // We check if we should show a rationale
                boolean canStillAskPermissions = shouldShowRequestPermissionRationale( Manifest.permission.RECORD_AUDIO );

                // We use the compat API for the Audio permissions
                if( canStillAskPermissions ){
                    ActivityCompat.requestPermissions( ControlActivity.this /* Context */, new String[]{ Manifest.permission.RECORD_AUDIO },
                            REQUEST_CODE_RECORD_AUDIO);
                }else{
                    Toast.makeText( ControlActivity.this, R.string.record_audio_permissions_fallback, Toast.LENGTH_SHORT ).show();
                }
            }
        }
    }

    /**
     * Obtains the device to resumeConnection via the Intent extras
     * */
    private BluetoothDevice getExtraDeviceToConnect(){
        if( getIntent() == null ){
            throw new IllegalStateException("You need to provide a bluetooth device via: "
                    + EXTRA_ARG_BLUETOOTH_DEVICE );
        }else{
            BluetoothDevice device = getIntent().getParcelableExtra( EXTRA_ARG_BLUETOOTH_DEVICE );
            if( device == null ){
                throw new IllegalStateException("You need to provide a bluetooth device via: "
                        + EXTRA_ARG_BLUETOOTH_DEVICE );
            }else{
                return device;
            }
        }
    }

    /**
     * Starts the connection with the bluetooth device
     * */
    public void resumeConnection() {
        // We create the connection
        showProgressDialog();
        new GetSocketTask( device ).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
    }


    /**
     * Sends a command to the device via the current bluetooth socket
     * */
    private void sendCommand( int command ){
        boolean commandSendSuccess = false;
        try{
            if( bTSocket != null && bTSocket.isConnected() ){
                if( connectionStream == null ){
                    connectionStream = bTSocket.getOutputStream();
                }

                connectionStream.write( command );
                commandSendSuccess = true;
            }
        }catch ( Exception e ){
            e.printStackTrace();
            Log.e(LOG_TAG, "Error sending the command..");
        }

        if( !commandSendSuccess ){
            Toast.makeText( ControlActivity.this /* context */, R.string.command_send_error, Toast.LENGTH_SHORT ).show();
            showConnectionErrorDialog();
        }
    }


    /**
     * Closes any bluetooth connection available
     * */
    public boolean closeConnection() {
        try {
            if( bTSocket != null && bTSocket.isConnected() ){
                if( connectionStream != null ){
                    connectionStream.close();
                }

                bTSocket.close();
            }
            return true;
        } catch(IOException e) {
            Log.d(LOG_TAG,"Could not close connection:" + e.toString());
            return false;
        }
    }

    /**
     * Shows a connection fallback dialog
     * */
    private void showConnectionErrorDialog(){
        ConnectionErrorDialogFragment errorDialogFragment = ConnectionErrorDialogFragment.newInstance();
        errorDialogFragment.show( getSupportFragmentManager(), FRAG_TAG_CONNECTION_ERROR_DIALOG );
    }

    /* -- Callback for the connection error consume -- */
    @Override
    public void onDialogCancelled() {
        // We just close the activity
        finish();
    }

    /* -- Callback from the VoiceCommandDialogFragment -- */
    @Override
    public void onCommandViaAlexa(int lexCommand) {
        sendCommand( lexCommand );
    }

    /**
     * Opening the progress dialog
     * */
    private void showProgressDialog(){
        ProgressDialogFragment progressDialogFragment = ProgressDialogFragment.newInstance();
        progressDialogFragment.show( getSupportFragmentManager(), FRAG_TAG_PROGRESS_DIALOG );
    }

    /**
     * Closing the progress dialog
     * */
    private void closeProgressDialog(){
        ProgressDialogFragment progressDialogFragment = (ProgressDialogFragment) getSupportFragmentManager()
                .findFragmentByTag( FRAG_TAG_PROGRESS_DIALOG );
        if( progressDialogFragment != null ){
            progressDialogFragment.dismissAllowingStateLoss();
        }
    }

    /**
     * Obtaining the socket connection in another thread
     * */
    public class GetSocketTask extends AsyncTask<Void, Void, BluetoothSocket>{

        // Reference for the task device
        private BluetoothDevice taskDevice;

        /**
         * Base constructor for the task
         * */
        public GetSocketTask( BluetoothDevice device ){
            this.taskDevice = device;
        }

        @Override
        protected BluetoothSocket doInBackground(Void... params) {
            try {
                BluetoothSocket socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(taskDevice,2);
                socket.connect();

                return socket;
            } catch(Exception e) {
                Log.d(LOG_TAG,"Could not resumeConnection: " + e.toString());

                closeConnection();
                return null;
            }
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            super.onPostExecute(bluetoothSocket);

            // We remove the progress dialog
            bTSocket = bluetoothSocket;
            closeProgressDialog();
        }
    }

}
