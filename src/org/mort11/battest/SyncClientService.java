package org.mort11.battest;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class SyncClientService extends IntentService {
	protected final UUID syncUUID = new UUID(
			getResources().getIntArray(R.array.uuid)[0], getResources()
					.getIntArray(R.array.uuid)[1]);
	protected Random rand;
	
	public static final Intent statusUpdate = new Intent("org.mort11.battest.SYNCED");
	public static final Intent failed = new Intent("org.mort11.battest.FAIL");

	public SyncClientService(String name) {
		super(name);
		rand = new Random();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		BluetoothDevice server = BluetoothAdapter.getDefaultAdapter()
				.getRemoteDevice(intent.getStringExtra("MAC"));
		try {
			while (true) {
				BluetoothSocket sock = server
						.createRfcommSocketToServiceRecord(syncUUID);
				sock.connect();
				byte[] buffer = new byte[1024];
				for (int i = 0; i < 50; i++) {
					rand.nextBytes(buffer);
					sock.getOutputStream().write(buffer);
				}
				sock.close();
				LocalBroadcastManager.getInstance(this).sendBroadcast(statusUpdate);
				this.wait(1000 * 180);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// Test stopped
		}
		LocalBroadcastManager.getInstance(this).sendBroadcast(failed);
	}

}
