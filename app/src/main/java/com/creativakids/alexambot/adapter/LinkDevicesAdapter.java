package com.creativakids.alexambot.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.creativakids.alexambot.R;

import java.util.List;

/**
 * Adapter for the list of devices
 *
 * Created by Mauricio Lara on 6/25/17.
 */
public class LinkDevicesAdapter extends RecyclerView.Adapter<LinkDevicesAdapter.DeviceViewHolder> implements View.OnClickListener{

    // Reference for the list of devices
    private List<BluetoothDevice> deviceList;

    // Reference from the calling context
    private LayoutInflater layoutInflater;

    // Reference for the adapter callback
    private AdapterCallback adapterCallback;

    /**
     * Base constructor
     * */
    public LinkDevicesAdapter( @NonNull Context context, List<BluetoothDevice> devices ){
        // Setting the calling context reference
        layoutInflater = LayoutInflater.from( context );

        // Setting the data
        deviceList = devices;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // We create the view holder
        View deviceView = layoutInflater.inflate( R.layout.item_device, parent, false /* attachToRoot */);
        DeviceViewHolder viewHolder = new DeviceViewHolder( deviceView );

        // Setting the click reference for the device
        viewHolder.baseView.setOnClickListener( LinkDevicesAdapter.this /* View.OnClickListener */);
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        // We fetch the related item
        BluetoothDevice positionDevice = deviceList.get( position );
        holder.txtDeviceName.setText( positionDevice.getName() );
        holder.txtDeviceID.setText( positionDevice.getAddress() );

        // Registering the reference device for the view
        holder.baseView.setTag( R.id.device, positionDevice );
    }

    @Override
    public int getItemCount() {
        return  deviceList == null ? 0 : deviceList.size();
    }

    /**
     * Setter method for the callback
     * */
    public void setAdapterCallback(AdapterCallback callback){
        adapterCallback = callback;
    }

    /**
     * Setter method for the adapter items
     * */
    public void updateDevices( List<BluetoothDevice> devices ){
        deviceList = devices;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        // Handling the click of the only kind of view
        BluetoothDevice device = (BluetoothDevice) v.getTag(R.id.device);
        if( device != null && adapterCallback != null ){
            adapterCallback.onLinkDevice( device );
        }
    }

    /**
     * Callback for the adapter
     * */
    public interface AdapterCallback{
        void onLinkDevice( BluetoothDevice device );
    }

    /**
     * View holder for the adapter
     * */
    class DeviceViewHolder extends RecyclerView.ViewHolder {

        // Reference for the view
        TextView txtDeviceName;
        TextView txtDeviceID;

        // Reference for the base view
        View baseView;

        /**
         * Base constructor for the device view holder
         * */
        DeviceViewHolder( @NonNull View view ){
            super( view );
            baseView = view;

            txtDeviceName = (TextView) view.findViewById( R.id.txt_device_name );
            txtDeviceID = (TextView) view.findViewById( R.id.txt_device_id );
        }
    }

}