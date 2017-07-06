package com.creativakids.alexambot.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.creativakids.alexambot.R;
import com.creativakids.alexambot.adapter.LinkDevicesAdapter;
import com.creativakids.alexambot.fragment.dialog.ConnectionConfirmationDialogFragment;
import com.creativakids.alexambot.view.SimpleDividerItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Activity in charge of the link of the mBots
 * <p>
 * Created by Mauricio Lara on 6/25/17.
 */
public class LinkActivity extends AppCompatActivity implements ConnectionConfirmationDialogFragment.Callback,
        LinkDevicesAdapter.AdapterCallback {

    // Logging tag
    private static final String LOG_TAG = LinkActivity.class.getSimpleName();

    // Child fragments tags
    private static final String FRAG_TAG_CONNECTION_CONFIRMATION_DIALOG = "FRAG_TAG_CONNECTION_CONFIRMATION_DIALOG";

    // Request code for the location
    private static final int REQUEST_CODE_LOCATION = 100;

    // Reference for the views
    private ProgressBar loadingProgress;
    private RecyclerView devicesRecycler;


    // Reference for the Bluetooth Adapter
    private BluetoothAdapter bluetoothAdapter;

    // Reference for the adapter of the devices
    private ArrayList<BluetoothDevice> devices;
    private HashMap<String, BluetoothDevice> obtainedDevices;
    private LinkDevicesAdapter linkDevicesAdapter;

    // Request code for the enabling of the bluetooth
    private static final int REQUEST_ENABLE_BT = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting the content view
        setContentView(R.layout.activity_link);

        // Getting the references for the views
        loadingProgress = (ProgressBar) findViewById(R.id.progress_item_fetch);
        devicesRecycler = (RecyclerView) findViewById(R.id.recycler_devices);

        // Getting the instance of the bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Getting the cache of devices
        obtainedDevices = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // We validate that the bluetooth is enabled on activity resume
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // We trigger the update of the bluetooth scanner process.
            setUpBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Handling of the permissions request
        if( requestCode == REQUEST_CODE_LOCATION ){
            // We check if the permissions were granted (two values)
            boolean werePermissionsApproved = grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED;
            if( werePermissionsApproved ){
                // We continue with the bluetooth flow
                setUpBluetooth();
            }else{
                // We show an error state
                Toast.makeText( LinkActivity.this, R.string.location_permissions_fallback, Toast.LENGTH_SHORT ).show();
                finish();
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up the bluetooth connection for the device
     * */
    private void setUpBluetooth(){
        // We check if we have the permissions
        boolean havePermissions = (ContextCompat.checkSelfPermission(LinkActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(LinkActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
        if( !havePermissions ){
            // We request the permissions
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // We check if we should show a rationale
                boolean canStillAskPermissions = shouldShowRequestPermissionRationale( Manifest.permission.ACCESS_COARSE_LOCATION );

                // We use the compat API for the Audio permissions
                if( canStillAskPermissions ){
                    ActivityCompat.requestPermissions( LinkActivity.this /* Context */, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION },
                            REQUEST_CODE_LOCATION);
                }else{
                    Toast.makeText( LinkActivity.this, R.string.location_permissions_fallback, Toast.LENGTH_SHORT ).show();
                    finish();
                }
            }

            return; // We cannot continue the bluetooth setup
        }


        startBluetoothListening();
        bluetoothAdapter.startDiscovery();

        // Setting first the list of bonded devices
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if( bondedDevices != null && !bondedDevices.isEmpty() ){
            for( BluetoothDevice device : bondedDevices ){
                // We filter the non make block devices
                if(TextUtils.isEmpty( device.getName()) || !device.getName().contains("Makeblock")
                        || device.getType() == BluetoothDevice.DEVICE_TYPE_LE){
                    return; // Nothing to handle...
                }

                if( devices == null ){
                    devices = new ArrayList<>();
                }

                // We check if we have already added the device
                BluetoothDevice obtainedDevice = obtainedDevices.get( device.getAddress() );
                if( obtainedDevice == null ){
                    devices.add( device );
                    obtainedDevices.put( device.getAddress(), device );
                }

                if( !devices.isEmpty() ){
                    updateAdapter();
                }
            }
        }
    }

    /**
     * Triggers the receiver for the listening of bluetooth devices
     * */
    private void startBluetoothListening(){
        Toast.makeText( LinkActivity.this,"Iniciando obtención de dispositivos", Toast.LENGTH_SHORT ).show();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(devicesUpdateReceiver, filter );
    }

    /**
     * Receiver for the bluetooth devices in place
     */
    private final BroadcastReceiver devicesUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Getting the new device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Logging the devices
                Log.d(LOG_TAG, "Device: " + device.getName() + " : " + device.getType() );

                // We filter the non make block devices
                if(TextUtils.isEmpty( device.getName()) || !device.getName().contains("Makeblock")
                        || device.getType() == BluetoothDevice.DEVICE_TYPE_LE){
                    return; // Nothing to handle...
                }

                if( devices == null ){
                    devices = new ArrayList<>();
                }

                // We check if we have already added the device
                BluetoothDevice obtainedDevice = obtainedDevices.get( device.getAddress() );
                if( obtainedDevice == null ){
                    devices.add( device );
                    obtainedDevices.put( device.getAddress(), device );
                }

                updateAdapter();

                // We update the visibility of the views list
                if (devices.isEmpty()) {
                    loadingProgress.setVisibility(View.VISIBLE);
                    devicesRecycler.setVisibility(View.GONE);
                } else {
                    loadingProgress.setVisibility(View.GONE);
                    devicesRecycler.setVisibility(View.VISIBLE);
                }

            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        // We stop the refresh of the handler
        try{
            unregisterReceiver(devicesUpdateReceiver);
        }catch ( Exception e ){
            Log.e(LOG_TAG, "Receiver not registered");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            // We check the result of the bluetooth update
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // We setup the bluetooth connection
                    setUpBluetooth();
                    break;
                case Activity.RESULT_CANCELED:
                    // We show an error message
                    closeAppBluetoothDisabled();
                    break;
                default:
                    // Do nothing...
            }
        } else {
            // We let the system handle this...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Updates the adapter reference according to the devices list..
     * */
    private void updateAdapter(){
        // We handle the update of the recycler list
        if (linkDevicesAdapter == null) {
            linkDevicesAdapter = new LinkDevicesAdapter(LinkActivity.this /* context */, devices);
            linkDevicesAdapter.setAdapterCallback( LinkActivity.this /* AdapterCallback */);
            devicesRecycler.addItemDecoration( new SimpleDividerItemDecoration( LinkActivity.this /* context */));
            devicesRecycler.setLayoutManager(new LinearLayoutManager(LinkActivity.this /* context */));
            devicesRecycler.setAdapter(linkDevicesAdapter);
        } else {
            // We update the list
            linkDevicesAdapter.updateDevices(devices);
        }
    }

    /**
     * Shows a message showing an exit confirmation for the app
     */
    private void closeAppBluetoothDisabled() {
        AlertDialog.Builder closeDialogBuilder = new AlertDialog.Builder(LinkActivity.this /* context */)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_bluetooth_not_enabled)
                .setMessage(R.string.dialog_message_bluetooth_not_enabled);
        closeDialogBuilder.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog closeDialog = closeDialogBuilder.create();
        closeDialog.show();

        // Setting the color for the positive button
        closeDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(LinkActivity.this /* context */, R.color.colorAccent));
    }

    /* -- Callback for the LinkDevicesAdapter -- */
    @Override
    public void onLinkDevice(BluetoothDevice device) {
        // We show a confirmation dialog for the connection
        ConnectionConfirmationDialogFragment confirmationDialog =
                ConnectionConfirmationDialogFragment.newInstance(device);
        confirmationDialog.show(getSupportFragmentManager(), FRAG_TAG_CONNECTION_CONFIRMATION_DIALOG);
    }

    /* -- Callbacks for the ConnectionConfirmationDialogFragment -- */
    @Override
    public void onConnectToDevice(BluetoothDevice bluetoothDevice) {
        // We start the connection to the given device in the next activity
        Intent goToControl = new Intent(LinkActivity.this /* context */, ControlActivity.class);
        goToControl.putExtra(ControlActivity.EXTRA_ARG_BLUETOOTH_DEVICE, bluetoothDevice);
        startActivity(goToControl);
    }

}
