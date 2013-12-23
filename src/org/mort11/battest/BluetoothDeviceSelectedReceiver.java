package org.mort11.battest;

import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

public class BluetoothDeviceSelectedReceiver extends BroadcastReceiver
{
	private BluetoothDevice partner;
	
	@Override
	public void onReceive(Context p1, Intent p2)
	{
		p1.unregisterReceiver(this);
		partner = (BluetoothDevice)p2.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	}
	
	public BluetoothDevice getPartner(){
		return partner;
	}
}
