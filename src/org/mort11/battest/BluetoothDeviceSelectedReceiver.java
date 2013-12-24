package org.mort11.battest;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothDeviceSelectedReceiver extends BroadcastReceiver
{
	private BluetoothDevice partner;
	
	@Override
	public void onReceive(Context p1, Intent p2)
	{
		
		p1.unregisterReceiver(this);
		partner = (BluetoothDevice)p2.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		Log.d(p1.getString(R.string.LogcatTag), "Partner = " + partner.getName());
	}
	
	public BluetoothDevice getPartner(){
		return partner;
	}
}
