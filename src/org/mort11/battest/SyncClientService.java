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
import android.util.Log;

public class SyncClientService extends IntentService {
	protected final UUID syncUUID = UUID.fromString("439a369c-5b92-48c2-90e7-a4a94dd4cea2"); 
	protected Random rand;

	public static final Intent statusUpdate = new Intent(
			"org.mort11.battest.SYNCED");
	public static final Intent failed = new Intent("org.mort11.battest.FAIL");

	private BluetoothSocket sock = null;
	
	public SyncClientService() {
		super(SyncClientService.class.getName());
		rand = new Random();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
		BluetoothDevice server = BluetoothAdapter.getDefaultAdapter()
				.getRemoteDevice(intent.getStringExtra("MAC"));
//		server.fetchUuidsWithSdp();
		
		try {
			while (true) {
				sock = server
						.createRfcommSocketToServiceRecord(syncUUID);
				Log.i(getString(R.string.LogcatTag), "Connecting");
				sock.connect();
				Log.i(getString(R.string.LogcatTag), "Connected");
				byte[] buffer = new byte[1024];
				for (int i = 0; i < 50; i++) {
					rand.nextBytes(buffer);
					sock.getOutputStream().write(buffer);
				}
				Log.i(getString(R.string.LogcatTag), "Sent Data");
				int bytesRX = 0;
				while (bytesRX < 50 * 1024) {
					int thisread = sock.getInputStream().read(buffer);
					if(thisread <= 0){
						throw new IOException("HUP");
					}
					bytesRX += thisread;
				}
				Log.i(getString(R.string.LogcatTag), "Read Data");
				sock.getOutputStream().write("DONE".getBytes());
				bytesRX = 0;
				while (bytesRX < 4) {
					try {
						if(sock.getInputStream().read() == -1){
							throw new IOException("HUP");
						}
						bytesRX++;
					} catch (IOException e) {
						break;
					}
				}
				Log.i(getString(R.string.LogcatTag), "DONE");
				try {
					sock.close();
				} catch (IOException e) {

				}
				LocalBroadcastManager.getInstance(this).sendBroadcast(
						statusUpdate.putExtra("success", true));
				Thread.sleep(1000 * 60 * 3);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				statusUpdate.putExtra("success", false));
		if(sock != null){
			try {
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.onDestroy();
		Log.i(getString(R.string.LogcatTag), "EXITING CLIENT");
	}

}
