package org.mort11.battest;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class SyncServerService extends IntentService {

	private BluetoothSocket syncSock = null;
	private BluetoothServerSocket servSock = null;
	
	protected final UUID syncUUID = UUID
			.fromString("439a369c-5b92-48c2-90e7-a4a94dd4cea2");

	protected Random rand;

	public static final Intent statusUpdate = new Intent(
			"org.mort11.battest.SYNCED");

	public SyncServerService() {
		super(SyncServerService.class.getName());
		rand = new Random();
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
		mAdapter.cancelDiscovery();
		try {
			servSock = mAdapter.listenUsingRfcommWithServiceRecord(
					"StupidSync", syncUUID);
			while (true) {
				syncSock = servSock.accept();
				Log.i(getString(R.string.LogcatTag), "Connected");
				byte[] buffer = new byte[1024];
				for (int i = 0; i < 50; i++) {
					rand.nextBytes(buffer);
					syncSock.getOutputStream().write(buffer);
				}
				int bytesRX = 0;
				while (bytesRX < 50 * 1024) {
					int thisread = syncSock.getInputStream().read(buffer);
					if(thisread <= 0){
						throw new IOException("HUP");
					}
					bytesRX += thisread;
				}
				syncSock.getOutputStream().write("DONE".getBytes());
				bytesRX = 0;
				while (bytesRX < 4) {
					try {
						if(syncSock.getInputStream().read() == -1){
							throw new IOException("HUP");
						}
						bytesRX++;
					} catch (IOException e) {
						break;
					}
				}
				try {
					syncSock.close();
				} catch (IOException e) {

				}
				LocalBroadcastManager.getInstance(this).sendBroadcast(
						statusUpdate.putExtra("success", true));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		LocalBroadcastManager.getInstance(this).sendBroadcast(
				statusUpdate.putExtra("success", false));
		if(syncSock != null ){
			try {
				syncSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(servSock != null){
			try {
				servSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.onDestroy();
		Log.i(getString(R.string.LogcatTag), "EXITING SERVER");
		
	}

}
